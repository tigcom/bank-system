package com.example.account_service.dto.request;

import com.example.common_service.constant.AccountStatus;
import com.example.common_service.constant.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfirmRequestDTO {


    @NotBlank(message = "OTP code is not blank")
    private String otpCode;

    @NotNull(message = "SavingRequestID  is required")
    private String savingRequestID;


}
