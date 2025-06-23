package com.example.mock_provider_server.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProviderPaymentResponse {
    private String status;
    private String providerTransactionId;
    private String message;
}
