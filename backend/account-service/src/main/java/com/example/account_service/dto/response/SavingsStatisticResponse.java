package com.example.account_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingsStatisticResponse {
    private Long totalSavingsAccounts;
    private BigDecimal totalSavingsBalance;
    private Map<Integer, Long> accountsByTerm; // Phân phối theo kỳ hạn (tháng)
    private Map<String, Long> accountsByInterestPaymentType; // MONTHLY, MATURITY
    private Map<String, Long> accountsByRenewOption; // AUTO_RENEW, NO_RENEW, etc.
    private Long accountsNearMaturity; // Tài khoản sắp đến hạn (trong 30 ngày)
    private BigDecimal averageBalance;
} 