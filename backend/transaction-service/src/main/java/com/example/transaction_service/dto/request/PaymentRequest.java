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
@Builder
@Schema(description = "Yêu cầu thanh toán hóa đơn từ tài khoản khách hàng")
public class PaymentRequest {

    @Schema(description = "Số tài khoản dùng để thanh toán", example = "100000001", required = true)
    @NotBlank(message = "From account number must not be blank")
    private String fromAccountNumber;

    @Schema(description = "Số tiền thanh toán", example = "150000", required = true)
    @NotNull(message = "Amount must not be null")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @Schema(description = "Loại tiền tệ (VND, USD, EUR)", example = "VND", required = true)
    @NotBlank(message = "Currency must not be blank")
    @Pattern(regexp = "VND|USD|EUR", message = "Currency must be VND, USD, or EUR")
    private String currency;

    @Schema(description = "Nội dung thanh toán (tuỳ chọn)", example = "Thanh toán tiền điện", required = false)
    @Size(max = 255, message = "Description must be at most 255 characters")
    private String description;
}
