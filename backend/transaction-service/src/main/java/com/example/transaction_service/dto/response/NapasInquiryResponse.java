package com.example.transaction_service.dto.response;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class NapasInquiryResponse {
    private String customerName;
    private String accountStatus;
}
