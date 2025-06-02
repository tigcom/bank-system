package com.example.account_service.exception;

public enum ErrorCode {

    // Account-related error codes
    USER_EXISTED(1001, "account.existed"),
    USER_NOTEXISTED(1002, "account.not.existed"),
    USERNAME_BLANK(1003, "account.username.blank"),
    PASSWORD_BLANK(1004, "account.password.blank"),
    USERNAME_INVALID(1005, "account.username.invalid"),
    INVALID_PASSWORD(1006, "account.password.invalid"),
    INVALID_CONFIRMPASSWORD(1007, "error.invalidconfirmpassword"),
    UNAUTHENTICATED(1008, "error.unauthenticated"),
    FILE_INVALID(1009, "error.file.invalid"),
    FILE_EMPTY(1010, "error.file.empty"),
    CONFLICT_KEYCLOAK(1011, "account.keycloak.conflict"),
    INVALID_INPUT_KEYCLOAK(1012, "account.keycloak.badrequest"),
    USERNAME_EXISTED(1013, "account.username.existed"),
    EMAIL_EXISTED(1014, "account.email.existed"),
    UNCATERROR_ERROR(1015, "uncategorized.error"),
    CUSTOMER_NOTACTIVE(1016, "customer.notactive"),
    CREDIT_REQUEST_NOTEXISTED(1017, "credit.request.notexisted"),
    INCOME_INVALID(1018, "credit.income.invalid"),
    AGE_INVALID(1019, "credit.age.invalid"),
    CREDIT_REQUEST_STATUS_INVALID(1020, "credit.request.status.invalid"),
    BALANCE_NOT_ENOUGH(1021, "account.balance.notenough"),
    SAVING_REQUEST_NOTEXISTED(1022, "saving.request.notexisted"),
    OTP_EXPIRED(1023, "otp.expired"),
    OTP_WRONG_MANY(1024, "otp.wrong.many"),
    INVALID_OTP(1025, "otp.invalid"),
    UNAUTHORIZATED(401, "error.unauthorizated"),
    SAVING_REQUEST_INVALID_STATUS(1026, "saving.request.invalid.status"),
    
    // New error codes for better validation
    INVALID_REQUEST(1027, "request.invalid"),
    INVALID_ACCOUNT_NUMBER(1028, "account.number.invalid"),
    INVALID_AMOUNT(1029, "amount.invalid"),
    INVALID_TERM(1030, "term.invalid"),
    CUSTOMER_NOT_FOUND(1031, "customer.not.found"),
    ACCOUNT_NOT_FOUND(1032, "account.not.found"),
    CORE_BANKING_SERVICE_ERROR(1033, "core.banking.service.error"),
    TRANSACTION_FAILED(1034, "transaction.failed");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
