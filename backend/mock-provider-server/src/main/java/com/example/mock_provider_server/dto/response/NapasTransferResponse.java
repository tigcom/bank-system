package com.example.mock_provider_server.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NapasTransferResponse {
    private String status;
    private String gatewayTransactionId;
    private String message;
}
