package com.example.transaction_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu nạp tiền vào tài khoản")
@Builder
public class DepositRequest {

    @Schema(description = "Số tài khoản cần nạp tiền", example = "100000001", required = true)
    @NotBlank(message = "Account number must not be blank")
    private String toAccountNumber;

    @Schema(description = "Số tiền muốn nạp", example = "100000", required = true)
    @NotNull(message = "Amount must not be null")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @Schema(description = "Loại tiền tệ (VND, USD, EUR)", example = "VND", required = true)
    @NotBlank(message = "Currency must not be blank")
    @Pattern(regexp = "VND|USD|EUR", message = "Currency must be VND, USD, or EUR")
    private String currency;

    @Schema(description = "Nội dung giao dịch (tuỳ chọn)", example = "Nạp tiền tiết kiệm", required = false)
    @Size(max = 255, message = "Description must be at most 255 characters")
    private String description;
}
