package com.example.loan_service.dto.request;


import com.example.common_service.constant.LoanType;
import com.example.common_service.constant.LoanStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.*;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoanRequestDTO implements Serializable {
    private Long loanId;

    private String disbursementAccountNumber;

    @NotBlank(message = "{loan.repaymentAccountNumber.notBlank}")
    private String repaymentAccountNumber;

    @NotNull(message = "{loan.amount.notNull}")
    @DecimalMin(value = "0.01", inclusive = true, message = "{loan.amount.min}")
    private BigDecimal amount;

    @NotNull(message = "{loan.interestRate.notNull}")
    @DecimalMin(value = "0.0", inclusive = false, message = "{loan.interestRate.min}")
    private BigDecimal interestRate;

    @NotNull(message = "{loan.termMonths.notNull}")
    @Min(value = 1, message = "{loan.termMonths.min}")
    @Max(value = 360, message = "{loan.termMonths.max}")
    private Integer termMonths;

    private Long customerId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime approvedAt;

    @Enumerated(EnumType.STRING)
    private LoanStatus status;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "{loan.loanType.notNull}")
    private LoanType loanType;

    @DecimalMin(value = "0.0", inclusive = true, message = "{loan.paidAmount.min}")
    private BigDecimal paidAmount;

    // new fields for income proof
    private BigDecimal declaredIncome;

    private String pathFile;

    public com.example.common_service.dto.request.LoanRequestDTO toCommonDto() {
        return com.example.common_service.dto.request.LoanRequestDTO.builder()
                .loanId(this.loanId)
                .disbursementAccountNumber(this.disbursementAccountNumber)
                .repaymentAccountNumber(this.repaymentAccountNumber)
                .amount(this.amount)
                .interestRate(this.interestRate)
                .termMonths(this.termMonths)
                .customerId(this.customerId)
                .createdAt(this.createdAt)
                .approvedAt(this.approvedAt)
                .status(this.status)
                .loanType(this.loanType)
                .paidAmount(this.paidAmount)
                .declaredIncome(this.declaredIncome)
                .pathFile(this.pathFile)
                .build();
    }
}
