package com.example.transaction_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {
    @NotBlank(message = "From account number must not be blank")
    private String fromAccountNumber;

    @NotNull(message = "Amount must not be null")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotBlank(message = "Currency must not be blank")
    @Pattern(regexp = "VND|USD|EUR", message = "Currency must be VND, USD, or EUR")
    private String currency;

    @Size(max = 255, message = "Description must be at most 255 characters")
    private String description;
}
