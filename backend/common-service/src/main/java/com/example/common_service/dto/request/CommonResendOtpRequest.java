package com.example.common_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CommonResendOtpRequest {
    private String referenceCode;
    private String accountNumberRecipient;
}
