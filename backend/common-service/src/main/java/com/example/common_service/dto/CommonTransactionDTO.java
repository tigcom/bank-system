package com.example.common_service.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CommonTransactionDTO {
    private String fromAccountNumber;

    private String toAccountNumber;

    private BigDecimal amount;

    private String description;

    private LocalDateTime timestamp;

    private String status;

    private String type;

    private String currency;

    private String referenceCode;

    private String failedReason;
}
