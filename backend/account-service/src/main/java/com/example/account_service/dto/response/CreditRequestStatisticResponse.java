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
public class CreditRequestStatisticResponse {
    private Long totalCreditRequests;
    private Map<String, Long> requestsByStatus; // PENDING, APPROVED, REJECTED
    private Long pendingRequests;
    private Long approvedRequests;
    private Long rejectedRequests;
    private Double approvalRate; // Tỷ lệ phê duyệt
    private BigDecimal averageRequestedIncome;
    private Map<String, Long> requestsByCardType; // VISA, MASTER, etc.
} 