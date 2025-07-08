package com.example.transaction_service.enums;

import lombok.Getter;

@Getter
public enum TransactionType {
    TRANSFER("Chuyển tiền "),
    EXTERNAL_TRANSFER("Chuyển tiền liên ngân hàng"),
    DEPOSIT("Nạp tiền"),
    WITHDRAW("Rút tiền"),

    PAY_BILL("Thanh toán hóa đơn"),
    LOAN_PAYMENT("Thanh toán khoản vay"),
    REFUND("Hoàn tiền"),
    DISBURSEMENT("Giải ngân khoản vay"),
    PAY_INTEREST("Thanh toán lãi"),
    CREATE_ACCOUNT_SAVING("Mở tài khoản tiết kiệm"),
    WITHDRAW_ACCOUNT_SAVING("Rút tiền từ tài khoản tiết kiệm"),

    CORE_BANKING("Giao dịch Core Banking");

    private final String displayName;
    TransactionType(String displayName) {
        this.displayName = displayName;
    }
}