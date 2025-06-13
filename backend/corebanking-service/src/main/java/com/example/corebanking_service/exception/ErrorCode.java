package com.example.corebanking_service.exception;

public enum ErrorCode {

    // Account-related error codes
    USER_EXISTED(1001, "account.existed"),
    USER_NOTEXISTED(1002, "account.not.existed"),
    USERNAME_BLANK(1003, "account.username.blank"),
    PASSWORD_BLANK(1004, "account.password.blank"),
    USERNAME_INVALID(1005, "account.username.invalid"),
    INVALID_PASSWORD(1006, "account.password.invalid"),
    INVALID_CONFIRMPASSWORD(1007, "error.invalidconfirmpassword"),
    UNAUTHENTICATED(1007, "error.unauthenticated"),
    FILE_INVALID(1003, "error.file.invalid"),
    FILE_EMPTY(1003, "error.file.empty"),
    CONFLICT_KEYCLOAK(1002, "account.keycloak.conflict"),
    INVALID_INPUT_KEYCLOAK(1002, "account.keycloak.badrequest"),
    USERNAME_EXISTED(1002, "account.username.existed"),
    EMAIL_EXISTED(1002, "account.email.existed"),
    UNCATERROR_ERROR(1002, "uncategorized.error"),
    CUSTOMER_NOTACTIVE(1002, "customer.notactive"),
    CREDIT_REQUEST_NOTEXISTED(1002, "credit.request.notexisted"),
    CARTCREDIT_TYPE_NOTEXISTED(1002, "credit.carttype.notexisted"),
    UNAUTHORIZATED(401,"error.unauthorizated"),
    ACCOUNT_NOT_EXIST(400,"Tài khoản không tồn tại" ),
    INSUFFICIENT_FUNDS(400,"Số dư không đủ" ),
    INVALID_AMOUNT(400,"Số tiền giao dịch không hợp lệ"),
    BANK_CODE_VALID(400,"Mã ngân hàng không hợp lệ" );
    ;



    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    private int code;
    private String message;

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
