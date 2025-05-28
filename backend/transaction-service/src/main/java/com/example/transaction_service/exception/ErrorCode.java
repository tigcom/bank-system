package com.example.transaction_service.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    FROM_ACCOUNT_NOT_PAYMENT(400,"Tài khoản gửi không phải tài khoản thanh toán" ),
    TO_ACCOUNT_NOT_PAYMENT(400,"Tài khoản nhận không phải tài khoản thanh toán" ),
    TRANSACTION_NOT_EXIST(400,"Giao dịch không tồn tại" ),
    SAME_ACCOUNT_TRANSFER(400,"Số tài khoản nguồn và tài khoản đích không được trùng" ),
    FROM_ACCOUNT_NOT_EXIST(400,"Tài khoản nguồn không tồn tại" ),
    TO_ACCOUNT_NOT_EXIST(400,"Tài khoản đích không tồn tại" ),
    INVALID_TRANSACTION_TYPE(400,"Chưa có loại giao dịch" ),
    CORE_BANKING_UNAVAILABLE(400,"Có lỗi khi kiểm tra số dư" ),
    INSUFFICIENT_FUNDS(400,"Số dư không đủ" ),
    INVALID_AMOUNT(400,"Số tiền giao dịch không hợp lệ"),
    OTP_EXPIRED(400,"Mã OTP đã hết hạn" ),
    INVALID_OTP(400,"Mã OTP không đúng" ),
    INVALID_TRANSACTION_STATUS(400,"Trạng thái giao dịch không hợp lệ " ),
    BANK_CODE_VALID(400,"Mã ngân hàng không hợp lệ" );

    private final int code;
    private final String message;
    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
