package com.example.common_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MailTransactionDTO implements Serializable {
    private String name;
    private BigDecimal amount;
    private String referenceCode;
    private String toAccountNumber;
    private String toCustomerName;
    private LocalDateTime timestamp;
    private String description;
    private String recipientMail;
    private String subject;
}
