package com.example.transaction_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {
    @NotBlank(message = "{fromAccountNumber.notblank}")
    private String fromAccountNumber;

    @NotBlank(message = "{toAccountNumber.notblank}")
    private String toAccountNumber;

    @NotNull(message = "{amount.notnull}")
    @DecimalMin(value = "0.01", inclusive = true, message = "{amount.min}")
    private BigDecimal amount;

    @Size(max = 255, message = "{description.size}")
    private String description;

    @NotBlank(message = "{currency.notblank}")
    @Pattern(regexp = "VND|USD|EUR", message = "Currency must be VND, USD, or EUR")
    private String currency;
}
