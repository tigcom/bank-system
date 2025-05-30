package com.example.transaction_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
public class ConfirmTransactionRequest {
    @NotBlank(message = "{referenceCode.notblank}")
    private String referenceCode;
    @NotBlank(message = "{otpCode.notblank}")
    private String otpCode;
}
