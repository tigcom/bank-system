package com.example.transaction_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Yêu cầu gửi lại mã OTP")
public class ResendOtpRequest {
    @Schema(description = "Mã tham chiếu giao dịch", example = "TXN123456789", required = true)
    @NotBlank(message = "{referenceCode.notblank}")
    private String referenceCode;

    @Schema(description = "Tài khoản người nhận mail", example = "8392012112", required = true)
    @NotBlank(message = "{recipient.notblank}")
    private String accountNumberRecipient;
}
