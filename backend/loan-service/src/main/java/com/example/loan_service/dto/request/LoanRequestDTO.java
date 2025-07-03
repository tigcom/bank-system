package com.example.loan_service.dto.request;


import com.example.loan_service.models.LoanStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import jakarta.validation.constraints.*;
import lombok.NoArgsConstructor;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class LoanRequestDTO {
    private Long loanId;
    @NotBlank
    private String accountNumber;
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
    private InfoIncomeRequestDto infoIncome;
}