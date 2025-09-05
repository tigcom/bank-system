package com.example.transaction_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoDeductRequest {
    private String fromAccountNumber; // Repayment account
    private String toAccountNumber;   // Master account
    private BigDecimal amount;        // Số tiền cần trừ
    private String currency;          // Loại tiền tệ
    private String description;       // Mô tả giao dịch
    private Long loanId;              // ID khoản vay
    private Long repaymentId;         // ID kỳ trả nợ
} 