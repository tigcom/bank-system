package com.example.Notification_service.service.impl;

import com.example.Notification_service.service.NotificationService;
import com.example.common_service.dto.MailMessageDTO;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final ObjectMapper objectMapper;

    @Override
    @KafkaListener(topics = "send-mail-raw", groupId = "mail-group", containerFactory = "kafkaListenerContainerFactory")
    public void sendNotification(Message<byte[]> messagee) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            MailMessageDTO mailMessage = objectMapper.readValue(messagee.getPayload(), MailMessageDTO.class);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "utf-8");
            helper.setFrom("nguyenhoainam29.08.01@gmail.com");
            helper.setReplyTo("nguyenhoainam29.08.01@gmail.com");
            helper.setTo(mailMessage.getRecipient());
            helper.setSubject(mailMessage.getSubject());
            helper.setText(mailMessage.getBody(), true);
            mailSender.send(message);
        } catch (Exception e) {
            System.out.println("Lỗi khi xử lý message: " + e.getMessage());
        }
    }
    @Override
    @KafkaListener(topics = "send-mail-html", groupId = "mail-group", containerFactory = "kafkaListenerContainerFactory")
    public void sendDTO(Message<byte[]> messagee) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            MailMessageDTO mailMessage = objectMapper.readValue(messagee.getPayload(), MailMessageDTO.class);

            Context context = new Context();
            context.setVariable("name", mailMessage.getRecipientName() != null ? mailMessage.getRecipientName() : "bạn");
            context.setVariable("content", mailMessage.getBody());

            String htmlContent = templateEngine.process("dto-template", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "utf-8");
            helper.setFrom("nguyenhoainam29.08.01@gmail.com");
            helper.setTo(mailMessage.getRecipient());
            helper.setSubject(mailMessage.getSubject());
            helper.setText(htmlContent, true);

            mailSender.send(message);

        } catch (Exception e) {
            System.out.println("Lỗi khi xử lý message: " + e.getMessage());
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

            String htmlContent = templateEngine.process("otp-register-forgot-password-template", context);

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
            log.info("Đã gửi email OTP khôi phục mật khẩu tới: {}", mailMessage.getRecipient());

            Context context = new Context();
            context.setVariable("name", mailMessage.getRecipientName() != null ? mailMessage.getRecipientName() : "Bạn");
            context.setVariable("request", "khôi phục mật khẩu");
            context.setVariable("otp", mailMessage.getBody());
            context.setVariable("ttl", 5);

            String htmlContent = templateEngine.process("otp-register-forgot-password-template", context);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom("nguyenhoainam29.08.01@gmail.com");
            helper.setTo(mailMessage.getRecipient());
            helper.setSubject(mailMessage.getSubject());
            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
            log.info("Đã gửi email OTP khôi phục mật khẩu tới: {}", mailMessage.getRecipient());
        } catch (Exception e) {
            log.error("Lỗi khi gửi email OTP khôi phục mật khẩu: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể gửi email OTP khôi phục mật khẩu: " + e.getMessage(), e);
        }
    }
}

