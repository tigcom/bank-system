package com.example.transaction_service.dto.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NapasInquiryRequest {
    private String accountNumber;
    private String bankCode;
}
