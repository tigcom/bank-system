package com.example.common_service.dto.request;


import com.example.common_service.constant.LoanType;
import com.example.common_service.constant.LoanStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class LoanRequestDTO implements Serializable {
    private Long loanId;
    @NotBlank
    private String 	disbursementAccountNumber;
    @NotBlank
    private String 	repaymentAccountNumber;
    @NotNull
    private BigDecimal amount;
    @NotNull
    private BigDecimal interestRate;
    @NotNull @Min(1) @Max(360)
    private Integer termMonths;
    @NotNull
    private Long customerId;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime approvedAt;
    @Enumerated(EnumType.STRING)
    private LoanStatus status;
    @Enumerated(EnumType.STRING)
    private LoanType loanType ;
    // Số tiền đã trả (dùng cho updateAccountFromLoan khi repayment)
    private BigDecimal paidAmount;
}