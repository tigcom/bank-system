package com.example.transaction_service.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    FROM_ACCOUNT_NOT_PAYMENT(400,"Tài khoản gửi không phải tài khoản thanh toán" ),
    TO_ACCOUNT_NOT_PAYMENT(400,"Tài khoản nhận không phải tài khoản thanh toán" ),
    FROM_ACCOUNT_NOT_ACTIVE(400,"Tài khoản gửi không hoạt động" ),
    TO_ACCOUNT_NOT_ACTIVE(400,"Tài khoản nhận không hoạt động" ),
    FROM_CUSTOMER_NOT_ACTIVE(400,"Người gửi bị cấm hoặc không khả dụng" ),
    TO_CUSTOMER_NOT_ACTIVE(400,"Người nhận không khả dụng hoặc bị cấm" ),
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
    INVALID_ACCOUNT(400,"Tài khoản này không thuộc sở hữu của bạn" ),
    INVALID_BANK_CODE(400,"Ngân hàng nhận phải khác với ngân hàng của bạn" ),
    OTP_FAILED_TOO_MANY_TIMES(400,"Nhập sai OTP quá 3 lần" ),
    INVALID_TRANSACTION_STATUS(400,"Trạng thái giao dịch không hợp lệ " ),
    CUSTOMER_NOT_EXIST(400,"Khách hàng không tồn tại" ),
    TIME_OUT(400,"Quá thời gian cho phép của giao dịch" ),
    TRANSACTION_FAILED(400,"Giao dịch thất bại" ),
    UNSUPPORTED_OPERATION(400,"Loại bill chưa được hỗ tro" ),
    BILL_NOT_FOUND(400,"Không tìm thấy hóa đơn" ),
    PROVIDER_SERVER_ERROR(500,"Lỗi từ server" ),
    PROVIDER_PAYMENT_FAILED(400,"Thanh toán thất bại" ),
    BILL_PAID(400,"Hóa đơn đã được thanh toán" ),
    BANK_CODE_VALID(400,"Mã ngân hàng không hợp lệ" );

    private final int code;
    private final String message;
    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
