package com.example.transaction_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Yêu cầu xác nhận giao dịch bằng mã OTP")
public class ConfirmTransactionRequest {

    @Schema(description = "Mã tham chiếu giao dịch", example = "TXN123456789", required = true)
    @NotBlank(message = "{referenceCode.notblank}")
    private String referenceCode;

    @Schema(description = "Mã OTP xác nhận giao dịch", example = "839201", required = true)
    @NotBlank(message = "{otpCode.notblank}")
    private String otpCode;
}
