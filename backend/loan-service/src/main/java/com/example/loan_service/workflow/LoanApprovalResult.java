package com.example.loan_service.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanApprovalResult {
    private Long loanId;
    private String status;
    private String disbursementAccountNumber;
    private BigDecimal amount;
    private String transactionReference;
    private LocalDateTime approvedAt;
    private String errorMessage;
    private boolean success;
} 