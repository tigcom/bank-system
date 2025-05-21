package com.example.loan_service.dto.response;


import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class LoanResponseDTO {
    private Long loanId;
    private Long customerId;
    private String accountId;
    private BigDecimal amount;
    private BigDecimal interestRate;
    private Integer termMonths;
    private BigDecimal declaredIncome;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
}