package com.example.Notification_service.service.impl;

import com.example.Notification_service.service.NotificationService;
import com.example.common_service.dto.MailMessageDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private  final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Override
    @KafkaListener(topics = "send-mail-raw", groupId = "mail-group", containerFactory = "kafkaListenerContainerFactory")
    public void sendNotification(Message<byte[]> messagee) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            MailMessageDTO mailMessage = objectMapper.readValue(messagee.getPayload(), MailMessageDTO.class);

            Context context = new Context();
            context.setVariable("name", mailMessage.getRecipientName() != null ? mailMessage.getRecipientName() : "bạn");
            context.setVariable("content", mailMessage.getBody());

            String htmlContent = templateEngine.process("noti-template", context);

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
    @KafkaListener(topics = "send-mail-html", groupId = "mail-group", containerFactory = "kafkaListenerContainerFactory")
    public void sendOtp(Message<byte[]> messagee) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            MailMessageDTO mailMessage = objectMapper.readValue(messagee.getPayload(), MailMessageDTO.class);

            Context context = new Context();
            context.setVariable("name", mailMessage.getRecipientName() != null ? mailMessage.getRecipientName() : "bạn");
            context.setVariable("content", mailMessage.getBody());

            String htmlContent = templateEngine.process("otp-template", context);

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
}

