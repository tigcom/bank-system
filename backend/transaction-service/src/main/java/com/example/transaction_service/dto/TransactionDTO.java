package com.example.transaction_service.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDTO {
    private String id;

    @NotBlank(message = "From account number is required")
    private String fromAccountNumber;

    @NotBlank(message = "To account number is required")
    private String toAccountNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", inclusive = true, message = "Amount must be at least 0.01")
    private BigDecimal amount;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    @PastOrPresent(message = "Timestamp cannot be in the future")
    private LocalDateTime timestamp;

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "PENDING|SUCCESS|FAILED", message = "Status must be one of: PENDING, SUCCESS, FAILED")
    private String status;

    @NotBlank(message = "Transaction type is required")
    @Pattern(regexp = "TRANSFER|DEPOSIT|WITHDRAW", message = "Type must be one of: TRANSFER, DEPOSIT, WITHDRAW")
    private String type;

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "VND|USD|EUR", message = "Currency must be VND, USD, or EUR")
    private String currency;

    @NotBlank(message = "Reference code is required")
    @Size(max = 100, message = "Reference code must not exceed 100 characters")
    private String referenceCode;
}
