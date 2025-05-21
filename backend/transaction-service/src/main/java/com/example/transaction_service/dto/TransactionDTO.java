package com.example.transaction_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDTO {
    private String id;
    private String fromAccountId;
    private String toAccountId;
    private BigDecimal amount;
    private String description;
    private LocalDateTime timestamp;
    private String status; // PENDING, SUCCESS, FAILED
    private String type;   // TRANSFER, DEPOSIT, ..
    private String currency;
    private String referenceCode;
}
