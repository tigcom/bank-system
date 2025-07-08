package com.master.masterservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MasterCardResponse {
    private String status;  // SUCCESS, FAILED, ERROR, RATE_LIMITED, PROCESSING
    private String message; // Thông báo lỗi hoặc thành công chi tiết
    private String errorCode; // Mã lỗi cụ thể (ví dụ: KYC_FAILED, INVALID_CUSTOMER)
    private String cardNumber;
    private LocalDate expiryDate;
    private String cardToken;
    private String cardHolderName;

    // Enum cho các loại lỗi business logic
    public enum ErrorCode {
        // Business Logic Failures (KHÔNG retry)
        INVALID_CUSTOMER,
        DUPLICATE_REGISTRATION,
        BLACKLISTED_CUSTOMER,
        KYC_FAILED,
        INSUFFICIENT_CREDIT_SCORE,
        AGE_RESTRICTION,
        CITIZENSHIP_RESTRICTION,
        SANCTIONS_LIST_MATCH,
        COMPLIANCE_VIOLATION,
        INCOME_VERIFICATION_FAILED,

        // Technical Failures (CÓ THỂ retry)
        SYSTEM_UNAVAILABLE,
        DATABASE_ERROR,
        NETWORK_TIMEOUT,
        RATE_LIMITED,
        SERVICE_MAINTENANCE,

        // Temporary Issues (NÊN retry)
        PROCESSING_QUEUE_FULL,
        TEMPORARY_SERVICE_DEGRADATION,
        EXTERNAL_SERVICE_TIMEOUT
    }

}