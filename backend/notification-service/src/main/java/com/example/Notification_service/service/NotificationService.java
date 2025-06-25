package com.example.Notification_service.service;

import com.example.common_service.dto.MailMessageDTO;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.Message;

public interface NotificationService {
    void sendNotification(Message<byte[]> message);


    void sendDTO(Message<byte[]> messagee);

    void sendOtpRegister(Message<byte[]> message);

    void sendOtpForgotPassword(Message<byte[]> message);
}
