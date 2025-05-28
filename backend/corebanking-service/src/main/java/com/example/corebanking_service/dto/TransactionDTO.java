package com.example.corebanking_service.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionDTO {
    private String fromAccountNumber;

    private String toAccountNumber;

    private BigDecimal amount;

    private String description;

    private LocalDateTime timestamp;

    private String status;

    private String type;
}
