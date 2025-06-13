package com.example.transaction_service.enums;

public enum TransactionType {
    TRANSFER,           // Giao dịch chuyển tiền giữa tài khoản (có thể là giữa hai người dùng)
    DEPOSIT,            // Giao dịch nạp tiền vào tài khoản (ví dụ: nạp tiền mặt, chuyển khoản vào)
    WITHDRAW,           // Giao dịch rút tiền từ tài khoản (ví dụ: rút tiền mặt, ATM)
    INTERNAL_TRANSFER,  // Giao dịch chuyển tiền nội bộ (giữa các tài khoản của cùng một người dùng)
    PAY_BILL,           // Thanh toán khoản vay
    DISBURSEMENT,       //Giải ngân khoản vay
    CREATE_ACCOUNT_SAVING,
    EXTERNAL_TRANSFER,
    CORE_BANKING     ,   // Giao dịch phát sinh từ hệ thống ngân hàng lõi (core banking system)
    WITHDRAW_ACCOUNT_SAVING
    ;
}
