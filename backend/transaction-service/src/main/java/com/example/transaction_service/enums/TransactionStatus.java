package com.example.transaction_service.enums;

import lombok.Getter;

@Getter
public enum TransactionStatus {

    PENDING("Đang xử lý"),
    COMPLETED("Thành công"),
    FAILED("Thất bại");

    private final String displayName;

    TransactionStatus(String displayName) {
        this.displayName = displayName;
    }
}
