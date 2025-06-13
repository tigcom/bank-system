package com.example.account_service.dto.response;

import com.example.common_service.constant.AccountStatus;
import com.example.common_service.constant.AccountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequestResponse {
    private String id;
    private String cifCode;
    private AccountType accountType;
    private PaymentRequestStatus status;
    
    public enum PaymentRequestStatus {
        PENDING,
        APPROVED,
        REJECTED
    }
} 