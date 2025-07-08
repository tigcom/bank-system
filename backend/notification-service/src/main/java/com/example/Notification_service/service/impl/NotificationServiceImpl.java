package com.example.Notification_service.service.impl;

import com.example.Notification_service.service.NotificationService;
import com.example.Notification_service.service.ConnectionHealthService;
import com.example.common_service.dto.CommonTransactionDTO;
import com.example.common_service.dto.MailMessageDTO;
import com.example.common_service.dto.CreditNotificationDTO;
import com.example.common_service.dto.MailTransactionDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final ObjectMapper objectMapper;
    private final ConnectionHealthService connectionHealthService;

    @Override
    @KafkaListener(topics = "send-mail-raw", groupId = "mail-group", containerFactory = "kafkaListenerContainerFactory")
    public void sendNotification(Message<byte[]> messagee) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            MailMessageDTO mailMessage = objectMapper.readValue(messagee.getPayload(), MailMessageDTO.class);
            log.info("Sending raw email to: {}", mailMessage.getRecipient());

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "utf-8");
            helper.setFrom("nguyenhoainam29.08.01@gmail.com");
            helper.setReplyTo("nguyenhoainam29.08.01@gmail.com");
            helper.setTo(mailMessage.getRecipient());
            helper.setSubject(mailMessage.getSubject());
            helper.setText(mailMessage.getBody(), true);

            mailSender.send(message);
            log.info("Email sent successfully to: {}", mailMessage.getRecipient());
        } catch (Exception e) {
            log.error("Lỗi khi xử lý message: {}", e.getMessage(), e);
        }
    }

    @Override
    @KafkaListener(topics = "send-mail-html", groupId = "mail-group", containerFactory = "kafkaListenerContainerFactory")
    public void sendDTO(Message<byte[]> messagee) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            MailMessageDTO mailMessage = objectMapper.readValue(messagee.getPayload(), MailMessageDTO.class);
            log.info("Sending HTML email to: {}", mailMessage.getRecipient());

            Context context = new Context();
            context.setVariable("name", mailMessage.getRecipientName() != null ? mailMessage.getRecipientName() : "bạn");
            context.setVariable("content", mailMessage.getBody());

            String htmlContent = templateEngine.process("dto-template", context);

            // Retry mechanism for email sending
            sendEmailWithRetry(mailMessage, htmlContent, 3);

        } catch (Exception e) {
            log.error("Lỗi khi xử lý HTML message: {}", e.getMessage(), e);
        }
    }

    @Override
    @KafkaListener(topics = "send-mail-transaction", groupId = "mail-transaction-group", containerFactory = "kafkaListenerContainerFactory")
    public void notificationTransaction(Message<byte[]> messagee) {
        try {
            ObjectMapper objectMapper = new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .disable(SerializationFeature
                            .WRITE_DATES_AS_TIMESTAMPS);
            MailTransactionDTO mailMessage = objectMapper.readValue(messagee.getPayload(), MailTransactionDTO.class);
            log.info("Sending HTML email to: {}", mailMessage.getName());
            System.out.println("Gửi mail tới:"+mailMessage.getRecipientMail());
            Context context = new Context();
            context.setVariable("name", mailMessage.getName() != null ? mailMessage.getName() : "bạn");
            context.setVariable("amount", mailMessage.getAmount());
            context.setVariable("referenceCode", mailMessage.getReferenceCode());
            context.setVariable("toAccountNumber", mailMessage.getToAccountNumber());
            context.setVariable("toCustomerName", mailMessage.getToCustomerName());
            context.setVariable("timestamp", mailMessage.getTimestamp());
            context.setVariable("description", mailMessage.getDescription());

            String htmlContent = templateEngine.process("notification-transaction", context);
            // Retry mechanism for email sending
            sendTransactionEmailWithRetry(mailMessage, htmlContent, 3);

        } catch (Exception e) {
            log.error("Lỗi khi xử lý HTML message: {}", e.getMessage(), e);
        }
    }

    private void sendEmailWithRetry(MailMessageDTO mailMessage, String htmlContent, int maxRetries) {
        // Check connection health before attempting to send
        if (!connectionHealthService.checkGmailConnection()) {
            log.error("Gmail SMTP not reachable. Running diagnostics...");
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
                log.info("HTML email sent successfully to: {} (attempt {})", mailMessage.getRecipient(), attempt);
                return; // Success, exit retry loop

            } catch (Exception e) {
                log.warn("Email send attempt {} failed for {}: {}", attempt, mailMessage.getRecipient(), e.getMessage());

                // If it's a connection reset, check connectivity again
                if (e.getMessage().contains("Connection reset")) {
                    log.warn("Connection reset detected, checking Gmail connectivity...");
                    boolean connected = connectionHealthService.checkGmailConnection();
                    log.info("Gmail connectivity check result: {}", connected);
                }

                if (attempt == maxRetries) {
                    log.error("Failed to send email to {} after {} attempts", mailMessage.getRecipient(), maxRetries, e);
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
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            CreditNotificationDTO notification = objectMapper.readValue(message.getPayload(), CreditNotificationDTO.class);
            log.info("Sending credit notification email to: {}", notification.getCustomerEmail());

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

            log.info("Đã gửi email thông báo credit request cho: {}", notification.getCustomerEmail());

        } catch (Exception e) {
            log.error("Lỗi khi gửi email credit notification: {}", e.getMessage(), e);
        }
    }

    @Override
    @KafkaListener(topics = "sentOtpRegister", groupId = "mail-group", containerFactory = "kafkaListenerContainerFactory")
    public void sendOtpRegister(Message<byte[]> message) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            MailMessageDTO mailMessage = objectMapper.readValue(message.getPayload(), MailMessageDTO.class);
            log.info("Đã gửi email OTP đăng ký tới: {}", mailMessage.getRecipient());
            Context context = new Context();
            context.setVariable("name", mailMessage.getRecipientName() != null ? mailMessage.getRecipientName() : "Bạn");
            context.setVariable("request", "đăng ký tài khoản");
            context.setVariable("otp", mailMessage.getBody());
            context.setVariable("ttl", 5);

            System.out.println(mailMessage.getBody());

            String htmlContent = templateEngine.process("otp-register-template", context);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom("nguyenhoainam29.08.01@gmail.com");
            helper.setTo(mailMessage.getRecipient());
            helper.setSubject(mailMessage.getSubject());
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
            log.info("Đã gửi email OTP đăng ký tới: {}", mailMessage.getRecipient());
        } catch (Exception e) {
            log.error("Lỗi khi gửi email OTP đăng ký tới: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể gửi email OTP đăng ký: " + e.getMessage(), e);
        }
    }

    @Override
    @KafkaListener(topics = "sentOtpForgotPassword", groupId = "mail-group", containerFactory = "kafkaListenerContainerFactory")
    public void sendOtpForgotPassword(Message<byte[]> message) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            MailMessageDTO mailMessage = objectMapper.readValue(message.getPayload(), MailMessageDTO.class);
            log.info("Nhận yêu cầu gửi email khôi phục mật khẩu cho: {}", mailMessage.getRecipient());

            // Tạo context cho Thymeleaf template
            Context context = new Context();
            context.setVariable("name", mailMessage.getRecipientName() != null ? mailMessage.getRecipientName() : "Bạn");
            context.setVariable("resetLink", mailMessage.getBody());

            // Xử lý template email HTML
            String htmlContent = templateEngine.process("reset-password-template", context);

            // Tạo email
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom("nguyenhoainam29.08.01@gmail.com");
            helper.setTo(mailMessage.getRecipient());
            helper.setSubject(mailMessage.getSubject());
            helper.setText(htmlContent, true); // true => là HTML

            // Gửi email
            mailSender.send(mimeMessage);
            log.info("Đã gửi email khôi phục mật khẩu tới: {}", mailMessage.getRecipient());

        } catch (Exception e) {
            log.error("Lỗi khi gửi email khôi phục mật khẩu: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể gửi email khôi phục mật khẩu: " + e.getMessage(), e);
        }
    }
    private void sendTransactionEmailWithRetry(MailTransactionDTO mailMessage, String htmlContent, int maxRetries) {
        // Check connection health before attempting to send
        if (!connectionHealthService.checkGmailConnection()) {
            log.error("Gmail SMTP not reachable. Running diagnostics...");
            connectionHealthService.logNetworkDiagnostics();
            throw new RuntimeException("Gmail SMTP server not reachable");
        }

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "utf-8");
                helper.setFrom("nguyenhoainam29.08.01@gmail.com");
                System.out.println("Mail nhận:"+(mailMessage.getRecipientMail()));
                helper.setTo(mailMessage.getRecipientMail());
                helper.setSubject(mailMessage.getSubject());
                helper.setText(htmlContent, true);

                mailSender.send(message);
                return; // Success, exit retry loop

            } catch (Exception e) {

                // If it's a connection reset, check connectivity again
                if (e.getMessage().contains("Connection reset")) {
                    log.warn("Connection reset detected, checking Gmail connectivity...");
                    boolean connected = connectionHealthService.checkGmailConnection();
                    log.info("Gmail connectivity check result: {}", connected);
                }

                if (attempt == maxRetries) {

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

}

