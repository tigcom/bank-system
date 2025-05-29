package com.example.common_service.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CommonDepositRequest {
    private String toAccountNumber;

    private BigDecimal amount;

    private String description;

    private String currency;
}
