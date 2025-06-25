package com.example.Notification_service.service;

public interface NotificationService {
    void sendNotification(byte[] payload);
    void sendDTO(byte[] payload);
    void sendOtpRegister(byte[] message);
    void sendOtpForgotPassword(byte[] message);
}
