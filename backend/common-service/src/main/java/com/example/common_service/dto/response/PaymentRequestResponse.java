package com.example.common_service.dto.response;

import com.example.common_service.constant.AccountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequestResponse implements Serializable {
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
