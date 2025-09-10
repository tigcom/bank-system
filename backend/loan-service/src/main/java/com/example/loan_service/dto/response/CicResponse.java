package com.example.loan_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CicResponse {
    private String status;      // success, failed, error
    private Integer creditScore;
    private Boolean overdue;
    private Integer debtGroup;
    private String errorCode;   // Error code for business logic failures
    private String message;     // Error message or additional info
    
    // Enum cho các loại lỗi business logic
    public enum ErrorCode {
        // Business Logic Failures (KHÔNG retry)
        INVALID_ID_NUMBER,
        ID_NOT_FOUND,
        BLACKLISTED_ID,
        DECEASED_PERSON,
        INVALID_PERSONAL_INFO,
        PRIVACY_RESTRICTION,
        LEGAL_RESTRICTION,
        COMPLIANCE_VIOLATION,
        
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