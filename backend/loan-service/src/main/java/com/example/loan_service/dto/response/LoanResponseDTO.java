package com.example.loan_service.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class LoanResponseDTO {
    private Long loanId;
    private Long customerId;
    private String accountNumber;
    private BigDecimal amount;
    private BigDecimal interestRate;
    private Integer termMonths;
    private BigDecimal declaredIncome;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
}