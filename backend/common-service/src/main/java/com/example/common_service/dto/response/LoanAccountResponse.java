package com.example.common_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanAccountResponse {
    private String accountNumber;
    private String cifCode;
    private String accountType;
    private String status;
    private LocalDate openedDate;
    private Long loanId;
    private BigDecimal loanAmount;
    private BigDecimal outstandingDebt;
    private BigDecimal interestRate;
    private Integer termMonths;
}


