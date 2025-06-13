package com.example.transaction_service.dto.request;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ProviderPaymentRequest {

    private String customerCode;

    private String billId;

    private BigDecimal amount;

    private String bankTransactionReference;

    private LocalDateTime paymentTimestamp;
}
