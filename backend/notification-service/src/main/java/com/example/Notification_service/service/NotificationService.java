package com.example.Notification_service.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.Message;

public interface NotificationService {
    void sendNotification(Message<byte[]> message);


    void sendOtp(Message<byte[]> messagee);
}
