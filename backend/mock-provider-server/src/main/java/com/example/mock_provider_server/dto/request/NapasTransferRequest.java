package com.example.mock_provider_server.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class NapasTransferRequest {
    private String toAccountNumber;
    private BigDecimal amount;
    private String description;
    private String bankCode;
}
