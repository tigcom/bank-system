package com.example.transaction_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu rút tiền từ tài khoản")
public class WithdrawRequest {

    @Schema(description = "Số tài khoản cần rút tiền", example = "100000001", required = true)
    @NotBlank(message = "{fromAccountNumber.notblank}")
    private String fromAccountNumber;

    @Schema(description = "Số tiền muốn rút", example = "500000", required = true)
    @NotNull(message = "{amount.notnull}")
    @DecimalMin(value = "0.01", inclusive = true, message = "{amount.min}")
    private BigDecimal amount;

    @Schema(description = "Nội dung giao dịch (tùy chọn)", example = "Rút tiền mặt tại quầy", required = false)
    @Size(max = 255, message = "{description.size}")
    private String description;

    @Schema(description = "Loại tiền tệ (VND, USD, EUR)", example = "VND", required = true)
    @NotBlank(message = "{currency.notblank}")
    @Pattern(regexp = "VND|USD|EUR", message = "{currency.pattern}")
    private String currency;
}
