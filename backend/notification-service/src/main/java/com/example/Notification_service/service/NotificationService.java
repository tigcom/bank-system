package com.example.Notification_service.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.Message;

public interface NotificationService {

    void sendNotification(byte[] payload);
    void sendDTO(byte[] payload);
    void sendOtpRegister(byte[] message);
    void sendOtpForgotPassword(byte[] message);
    @KafkaListener(topics = "send-mail-transaction", groupId = "mail-transaction-group", containerFactory = "kafkaListenerContainerFactory")
    void notificationTransaction(Message<byte[]> messagee);
    
    void sendLoanNotification(byte[] payload);
}
