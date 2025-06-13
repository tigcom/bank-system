package com.example.mock_provider_server.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProviderPaymentRequest {
    private String customerCode;
    private String billId;
    private BigDecimal amount;
    private String bankTransactionReference;
    private LocalDateTime paymentTimestamp;
}
