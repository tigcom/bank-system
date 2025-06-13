package com.example.transaction_service.enums;

import lombok.Getter;

@Getter
public enum TransactionType {
    TRANSFER("Chuyển tiền"),
    DEPOSIT("Nạp tiền"),
    WITHDRAW("Rút tiền"),
    INTERNAL_TRANSFER("Chuyển tiền nội bộ"),
    LOAN_PAYMENT("Thanh toán khoản vay"),
    PAY_BILL("Thanh toán hóa đơn"),
    REFUND("Hoàn tiền"),
    DISBURSEMENT("Giải ngân khoản vay"),
    CREATE_ACCOUNT_SAVING("Mở tài khoản tiết kiệm"),
    WITHDRAW_ACCOUNT_SAVING("Rút tiền từ tài khoản tiết kiệm"),
    EXTERNAL_TRANSFER("Chuyển tiền liên ngân hàng"),
    CORE_BANKING("Giao dịch Core Banking");

    private final String displayName;
    TransactionType(String displayName) {
        this.displayName = displayName;
    }
}
