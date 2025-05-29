package com.example.transaction_service.dto.response;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PayRepaymentResponse {
    private String referenceCode;
    private String status;
}
