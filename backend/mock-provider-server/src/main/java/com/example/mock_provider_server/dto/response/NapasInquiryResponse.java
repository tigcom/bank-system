package com.example.mock_provider_server.dto.response;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class NapasInquiryResponse {
    private String customerName;
    private String accountStatus;
}
