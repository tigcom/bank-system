package com.example.transaction_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu giải ngân khoản vay vào tài khoản khách hàng")
public class DisburseRequest {

    @Schema(description = "Số tài khoản nhận tiền giải ngân", example = "100000002", required = true)
    @NotBlank(message = "{toAccountNumber.notblank}")
    private String toAccountNumber;

    @Schema(description = "Số tiền cần giải ngân", example = "10000000", required = true)
    @NotNull(message = "{amount.notnull}")
    @DecimalMin(value = "0.01", inclusive = true, message = "{amount.min}")
    private BigDecimal amount;

    @Schema(description = "Nội dung giải ngân (tùy chọn)", example = "Giải ngân khoản vay tiêu dùng", required = false)
    @Size(max = 255, message = "{description.size}")
    private String description;

    @Schema(description = "Loại tiền tệ (VND, USD, EUR)", example = "VND", required = true)
    @NotBlank(message = "{currency.notblank}")
    @Pattern(regexp = "VND|USD|EUR", message = "Currency must be VND, USD, or EUR")
    private String currency;
}
