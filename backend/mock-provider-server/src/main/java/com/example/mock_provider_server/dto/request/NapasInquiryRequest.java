package com.example.mock_provider_server.dto.request;

import lombok.Data;

@Data
public class NapasInquiryRequest {
    private String accountNumber;
    private String bankCode;
}
