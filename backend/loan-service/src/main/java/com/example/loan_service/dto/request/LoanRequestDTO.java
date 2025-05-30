package com.example.loan_service.dto.request;


import lombok.Data;
import jakarta.validation.constraints.*;


import java.math.BigDecimal;

@Data
public class LoanRequestDTO {

    @NotNull(message = "customerId không được null")
    private Long customerId;

    @NotBlank(message = "accountNumber không được để trống")
    private String accountNumber;

    @NotNull(message = "amount không được null")
    @DecimalMin(value = "1000.00", message = "amount phải >= 1000.00")
    private BigDecimal amount;

    @NotNull(message = "interestRate không được null")
    @DecimalMin(value = "0.0", message = "interestRate phải >= 0.0")
    private BigDecimal interestRate;

    @NotNull(message = "termMonths không được null")
    @Min(value = 1, message = "termMonths phải >= 1")
    @Max(value = 360, message = "termMonths phải <= 360")
    private Integer termMonths;

    @DecimalMin(value = "0.0", message = "declaredIncome phải >= 0.0")
    private BigDecimal declaredIncome;
}