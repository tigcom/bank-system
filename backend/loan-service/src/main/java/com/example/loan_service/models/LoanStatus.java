package com.example.loan_service.models;

public enum LoanStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CLOSED,
    CANCELLED;
    public com.example.common_service.constant.LoanStatus toCommonStatus() {
        return com.example.common_service.constant.LoanStatus.valueOf(this.name());
    }
}
