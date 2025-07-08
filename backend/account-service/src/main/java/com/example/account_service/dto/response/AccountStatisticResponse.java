package com.example.account_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountStatisticResponse {
    private Long totalAccounts;
    private Map<String, Long> accountsByType;  // PAYMENT, CREDIT, SAVING, MASTER
    private Map<String, Long> accountsByStatus; // ACTIVE, CLOSED, MATURED, etc.
    private Long activeAccounts;
    private Long inactiveAccounts;
} 