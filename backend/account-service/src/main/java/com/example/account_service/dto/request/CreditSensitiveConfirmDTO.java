package com.example.account_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditSensitiveConfirmDTO {

    @NotBlank(message = "OTP code is not blank")
    private String otpCode;

    @NotNull(message = "Account number is required")
    private String accountNumber;

    @NotNull(message = "Temp request key is required")
    private String tempRequestKey;
} 