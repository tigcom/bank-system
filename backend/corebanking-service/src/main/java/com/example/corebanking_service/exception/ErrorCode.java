package com.example.corebanking_service.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    ACCOUNT_NOT_EXIST(400,"Tài khoản không tồn tại" ),
    INSUFFICIENT_FUNDS(400,"Số dư không đủ" ),
    INVALID_AMOUNT(400,"Số tiền giao dịch không hợp lệ"),
    BANK_CODE_VALID(400,"Mã ngân hàng không hợp lệ" );

    private final int code;
    private final String message;
    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
