package com.example.corebanking_service.dto.request;

import lombok.Data;

@Data
public class NapasInquiryRequest {
    private String accountNumber;
    private String bankCode;
}
