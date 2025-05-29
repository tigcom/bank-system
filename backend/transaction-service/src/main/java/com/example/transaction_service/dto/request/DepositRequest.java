package com.example.transaction_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class DepositRequest {

    @NotBlank(message = "{toAccountNumber.notblank}")
    private String toAccountNumber;

    @NotNull(message = "{amount.notnull}")
    @DecimalMin(value = "0.01", inclusive = true, message = "{amount.min}")
    private BigDecimal amount;

    @Size(max = 255, message = "{description.size}")
    private String description;

    @NotBlank(message = "{currency.notblank}")
    @Pattern(regexp = "VND|USD|EUR", message = "{currency.pattern}")
    private String currency;

}
