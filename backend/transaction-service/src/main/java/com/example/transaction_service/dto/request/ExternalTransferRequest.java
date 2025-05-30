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
@Schema(description = "Yêu cầu chuyển tiền liên ngân hàng từ tài khoản nguồn sang tài khoản đích")
public class ExternalTransferRequest {
    @Schema(description = "Số tài khoản gửi tiền", example = "100000001", required = true)
    @NotBlank(message = "{fromAccountNumber.notblank}")
    private String fromAccountNumber;

    @Schema(description = "Số tài khoản nhận tiền", example = "100000002", required = true)
    @NotBlank(message = "{toAccountNumber.notblank}")
    private String toAccountNumber;

    @Schema(description = "Số tiền chuyển", example = "500000", required = true)
    @NotNull(message = "{amount.notnull}")
    @DecimalMin(value = "0.01", inclusive = true, message = "{amount.min}")
    private BigDecimal amount;

    @Schema(description = "Nội dung giao dịch", example = "Chuyển tiền sinh hoạt", required = false)
    @Size(max = 255, message = "{description.size}")
    private String description;

    @Schema(description = "Loại tiền tệ (VND, USD, EUR)", example = "VND", required = true)
    @NotBlank(message = "{currency.notblank}")
    @Pattern(regexp = "VND|USD|EUR", message = "Currency must be VND, USD, or EUR")
    private String currency;

    @Schema(description = "Số tài khoản nhận tiền", example = "100000002", required = true)
    @NotBlank(message = "{destinationBankCode.notblank}")
    private String destinationBankCode;
    
}
