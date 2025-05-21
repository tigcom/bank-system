package com.example.transaction_service.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    BANK_CODE_VALID(400,"Mã ngân hàng không hợp lệ" );

    private final int code;
    private final String message;
    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
