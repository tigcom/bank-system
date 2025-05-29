package com.example.common_service.dto.request;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class CommonResendOtpRequest implements Serializable {
    private String referenceCode;
    private String accountNumberRecipient;
}
