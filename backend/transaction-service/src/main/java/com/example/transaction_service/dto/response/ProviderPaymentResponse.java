package com.example.transaction_service.dto.response;

import lombok.Data;

@Data
public class ProviderPaymentResponse {

    private String status;

    private String providerTransactionId;

    private String message;
}
