package com.example.Notification_service.service.impl;

import com.example.Notification_service.service.NotificationService;
import com.example.Notification_service.service.ConnectionHealthService;
import com.example.common_service.dto.MailMessageDTO;
import com.example.common_service.dto.CreditNotificationDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final ObjectMapper objectMapper;
    private final ConnectionHealthService connectionHealthService;

    @Override
    @KafkaListener(topics = "send-mail-raw", groupId = "mail-raw-group", containerFactory = "kafkaListenerContainerFactory")
    public void sendNotification(Message<byte[]> messagee) {
        String requestId = UUID.randomUUID().toString();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            MailMessageDTO mailMessage = objectMapper.readValue(messagee.getPayload(), MailMessageDTO.class);
            log.info("SEND_RAW_EMAIL_REQUEST - RequestId: {}, Recipient: {}", requestId, mailMessage.getRecipient());
            Context context = new Context();
            context.setVariable("name", mailMessage.getRecipientName() != null ? mailMessage.getRecipientName() : "bạn");
            context.setVariable("content", mailMessage.getBody());

            String htmlContent = templateEngine.process("noti-template", context);

            // Retry mechanism for email sending
            sendEmailWithRetry(mailMessage, htmlContent, 3,requestId);
            log.info("SEND_RAW_EMAIL_SUCCESS - RequestId: {}, Recipient: {}", requestId, mailMessage.getRecipient());
        } catch (Exception e) {
            log.error("SEND_RAW_EMAIL_FAILED - RequestId: {}, Error: {}", requestId, e.getMessage(), e);
        }
    }

    @Override
    @KafkaListener(topics = "send-mail-html", groupId = "mail-group", containerFactory = "kafkaListenerContainerFactory")
    public void sendDTO(Message<byte[]> messagee) {
        String requestId = UUID.randomUUID().toString();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            MailMessageDTO mailMessage = objectMapper.readValue(messagee.getPayload(), MailMessageDTO.class);
            log.info("SEND_HTML_EMAIL_REQUEST - RequestId: {}, Recipient: {}", requestId, mailMessage.getRecipient());

            Context context = new Context();
            context.setVariable("name", mailMessage.getRecipientName() != null ? mailMessage.getRecipientName() : "you");
            context.setVariable("content", mailMessage.getBody());

            String htmlContent = templateEngine.process("otp-template", context);

            // Retry mechanism for email sending
            sendEmailWithRetry(mailMessage, htmlContent, 3, requestId);

        } catch (Exception e) {
            log.error("SEND_HTML_EMAIL_FAILED - RequestId: {}, Error: {}", requestId, e.getMessage(), e);
        }
    }

    private void sendEmailWithRetry(MailMessageDTO mailMessage, String htmlContent, int maxRetries, String requestId) {
        // Check connection health before attempting to send
        if (!connectionHealthService.checkGmailConnection()) {
            log.error("GMAIL_SMTP_UNREACHABLE - RequestId: {}. Running diagnostics...", requestId);
            connectionHealthService.logNetworkDiagnostics();
            throw new RuntimeException("Gmail SMTP server not reachable");
        }

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "utf-8");
                helper.setFrom("nguyenhoainam29.08.01@gmail.com");
                helper.setTo(mailMessage.getRecipient());
                helper.setSubject(mailMessage.getSubject());
                helper.setText(htmlContent, true);

                mailSender.send(message);
                log.info("SEND_HTML_EMAIL_SUCCESS - RequestId: {}, Recipient: {}, Attempt: {}", requestId, mailMessage.getRecipient(), attempt);
                return; // Success, exit retry loop

            } catch (Exception e) {
                log.warn("SEND_HTML_EMAIL_ATTEMPT_FAILED - RequestId: {}, Attempt: {}, Recipient: {}, Error: {}", requestId, attempt, mailMessage.getRecipient(), e.getMessage());

                // If it's a connection reset, check connectivity again
                if (e.getMessage().contains("Connection reset")) {
                    log.warn("CONNECTION_RESET_DETECTED - RequestId: {}. Checking Gmail connectivity...", requestId);
                    boolean connected = connectionHealthService.checkGmailConnection();
                    log.info("GMAIL_CONNECTIVITY_CHECK_RESULT - RequestId: {}, Connected: {}", requestId, connected);
                }

                if (attempt == maxRetries) {
                    log.error("SEND_HTML_EMAIL_MAX_ATTEMPTS_FAILED - RequestId: {}, Recipient: {}, MaxRetries: {}", requestId, mailMessage.getRecipient(), maxRetries, e);
                    connectionHealthService.logNetworkDiagnostics();
                    throw new RuntimeException("Email sending failed after " + maxRetries + " attempts", e);
                }

                // Wait before retry with exponential backoff
                try {
                    Thread.sleep(2000 * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    @KafkaListener(topics = "send-credit-notification", groupId = "credit-notification-group", containerFactory = "kafkaListenerContainerFactory")
    public void sendCreditNotification(Message<byte[]> message) {
        String requestId = UUID.randomUUID().toString();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            CreditNotificationDTO notification = objectMapper.readValue(message.getPayload(), CreditNotificationDTO.class);
            log.info("SEND_CREDIT_NOTIFICATION_REQUEST - RequestId: {}, Recipient: {}", requestId, notification.getCustomerEmail());

            Context context = new Context();
            context.setVariable("customerName", notification.getCustomerName());
            context.setVariable("cardType", notification.getCardType());

            String templateName;
            if ("approval".equals(notification.getTemplateType())) {
                context.setVariable("accountNumber", notification.getAccountNumber());
                templateName = "credit-approval-template";
            } else {
                context.setVariable("rejectionReason", notification.getRejectionReason());
                templateName = "credit-rejection-template";
            }

            String htmlContent = templateEngine.process(templateName, context);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "utf-8");
            helper.setFrom("nguyenhoainam29.08.01@gmail.com");
            helper.setTo(notification.getCustomerEmail());
            helper.setSubject(notification.getSubject());
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);

            log.info("SEND_CREDIT_NOTIFICATION_SUCCESS - RequestId: {}, Recipient: {}", requestId, notification.getCustomerEmail());

        } catch (Exception e) {
            log.error("SEND_CREDIT_NOTIFICATION_FAILED - RequestId: {}, Error: {}", requestId, e.getMessage(), e);
        }
    }

    @Override
    @KafkaListener(topics = "sentOtpRegister", groupId = "mail-group", containerFactory = "kafkaListenerContainerFactory")
    public void sendOtpRegister(Message<byte[]> message) {
        String requestId = UUID.randomUUID().toString();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            MailMessageDTO mailMessage = objectMapper.readValue(message.getPayload(), MailMessageDTO.class);
            log.info("SEND_OTP_REGISTER_REQUEST - RequestId: {}, Recipient: {}", requestId, mailMessage.getRecipient());
            Context context = new Context();
            context.setVariable("name", mailMessage.getRecipientName() != null ? mailMessage.getRecipientName() : "You");
            context.setVariable("request", "register account");
            context.setVariable("otp", mailMessage.getBody());
            context.setVariable("ttl", 5);

            String htmlContent = templateEngine.process("otp-register-template", context);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom("nguyenhoainam29.08.01@gmail.com");
            helper.setTo(mailMessage.getRecipient());
            helper.setSubject(mailMessage.getSubject());
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
            log.info("SEND_OTP_REGISTER_SUCCESS - RequestId: {}, Recipient: {}", requestId, mailMessage.getRecipient());
        } catch (Exception e) {
            log.error("SEND_OTP_REGISTER_FAILED - RequestId: {}, Error: {}", requestId, e.getMessage(), e);
            throw new RuntimeException("Unable to send OTP register email: " + e.getMessage(), e);
        }
    }

    @Override
    @KafkaListener(topics = "sentOtpForgotPassword", groupId = "mail-group", containerFactory = "kafkaListenerContainerFactory")
    public void sendOtpForgotPassword(Message<byte[]> message) {
        String requestId = UUID.randomUUID().toString();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            MailMessageDTO mailMessage = objectMapper.readValue(message.getPayload(), MailMessageDTO.class);
            log.info("SEND_OTP_FORGOT_PASSWORD_REQUEST - RequestId: {}, Recipient: {}", requestId, mailMessage.getRecipient());

            // Create context for Thymeleaf template
            Context context = new Context();
            context.setVariable("name", mailMessage.getRecipientName() != null ? mailMessage.getRecipientName() : "You");
            context.setVariable("resetLink", mailMessage.getBody());

            // Process HTML email template
            String htmlContent = templateEngine.process("reset-password-template", context);

            // Create email
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom("nguyenhoainam29.08.01@gmail.com");
            helper.setTo(mailMessage.getRecipient());
            helper.setSubject(mailMessage.getSubject());
            helper.setText(htmlContent, true); // true => HTML

            // Send email
            mailSender.send(mimeMessage);
            log.info("SEND_OTP_FORGOT_PASSWORD_SUCCESS - RequestId: {}, Recipient: {}", requestId, mailMessage.getRecipient());

        } catch (Exception e) {
            log.error("SEND_OTP_FORGOT_PASSWORD_FAILED - RequestId: {}, Error: {}", requestId, e.getMessage(), e);
            throw new RuntimeException("Unable to send forgot password email: " + e.getMessage(), e);
        }
    }

    @Override
    @KafkaListener(topics = "sentMailNotificationKyc", groupId = "mail-group", containerFactory = "kafkaListenerContainerFactory")
    public void sendNotificationKyc(Message<byte[]> message) {
        String requestId = UUID.randomUUID().toString();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            MailMessageDTO mailMessage = objectMapper.readValue(message.getPayload(), MailMessageDTO.class);
            log.info("SEND_NOTIFICATION_KYC_REQUEST - RequestId: {}, Recipient: {}", requestId, mailMessage.getRecipient());

            // Create context for Thymeleaf template
            Context context = new Context();
            context.setVariable("name", mailMessage.getRecipientName() != null ? mailMessage.getRecipientName() : "You");
            context.setVariable("body", mailMessage.getBody());

            // Process HTML email template
            String htmlContent = templateEngine.process("notification-kyc", context);

            // Create email
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom("nguyenhoainam29.08.01@gmail.com");
            helper.setTo(mailMessage.getRecipient());
            helper.setSubject(mailMessage.getSubject());
            helper.setText(htmlContent, true); // true => HTML

            // Send email
            mailSender.send(mimeMessage);
            log.info("SEND_NOTIFICATION_KYC_SUCCESS - RequestId: {}, Recipient: {}", requestId, mailMessage.getRecipient());

        } catch (Exception e) {
            log.error("SEND_NOTIFICATION_KYC_FAILED - RequestId: {}, Error: {}", requestId, e.getMessage(), e);
            throw new RuntimeException("Unable to send KYC notification email: " + e.getMessage(), e);
        }
    }

    @Override
    @KafkaListener(topics = "sentMailNotificationKycResult", groupId = "mail-group", containerFactory = "kafkaListenerContainerFactory")
    public void sendNotificationKycResult(Message<byte[]> message) {
        String requestId = UUID.randomUUID().toString();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            MailMessageDTO mailMessage = objectMapper.readValue(message.getPayload(), MailMessageDTO.class);
            log.info("SEND_NOTIFICATION_KYC_RESULT_REQUEST - RequestId: {}, Recipient: {}", requestId, mailMessage.getRecipient());

            // Create context for Thymeleaf template
            Context context = new Context();
            context.setVariable("name", mailMessage.getRecipientName() != null ? mailMessage.getRecipientName() : "You");
            context.setVariable("body", mailMessage.getBody());

            // Process HTML email template
            String htmlContent = templateEngine.process("notification-kyc", context);

            // Create email
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom("nguyenhoainam29.08.01@gmail.com");
            helper.setTo(mailMessage.getRecipient());
            helper.setSubject(mailMessage.getSubject());
            helper.setText(htmlContent, true); // true => HTML

            // Send email
            mailSender.send(mimeMessage);
            log.info("SEND_NOTIFICATION_KYC_RESULT_SUCCESS - RequestId: {}, Recipient: {}", requestId, mailMessage.getRecipient());

        } catch (Exception e) {
            log.error("SEND_NOTIFICATION_KYC_RESULT_FAILED - RequestId: {}, Error: {}", requestId, e.getMessage(), e);
            throw new RuntimeException("Unable to send KYC result notification email: " + e.getMessage(), e);
        }
    }
}

