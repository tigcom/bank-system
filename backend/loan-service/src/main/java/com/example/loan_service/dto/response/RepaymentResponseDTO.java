package com.example.loan_service.dto.response;


import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RepaymentResponseDTO {
    private Long repaymentId;
    private Long loanId;
    private LocalDate dueDate;
    private BigDecimal principal;
    private BigDecimal interest;
    private BigDecimal paidAmount;
    private String status;
}