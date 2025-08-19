package com.example.common_service.dto.request;

import com.example.common_service.constant.LoanType;
import com.example.common_service.constant.LoanStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
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
public class LoanRequestDTO implements Serializable {
    private Long loanId;

    @NotBlank(message = "{loan.disbursementAccountNumber.notBlank}")
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

    @NotNull(message = "{loan.customerId.notNull}")
    private Long customerId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime approvedAt;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "{loan.status.notNull}")
    private LoanStatus status;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "{loan.loanType.notNull}")
    private LoanType loanType;

    @DecimalMin(value = "0.0", inclusive = true, message = "{loan.paidAmount.min}")
    private BigDecimal paidAmount;

    // new fields for income proof
    private BigDecimal declaredIncome;
    private String pathFile;
}
