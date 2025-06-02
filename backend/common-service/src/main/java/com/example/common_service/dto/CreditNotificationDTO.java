package com.example.common_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditNotificationDTO implements Serializable {
    private String customerName;
    private String customerEmail;
    private String cardType;
    private String accountNumber;
    private String rejectionReason;
    private String templateType; // "approval" or "rejection"
    private String subject;
} 