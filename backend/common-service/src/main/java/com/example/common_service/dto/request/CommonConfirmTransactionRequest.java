package com.example.common_service.dto.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class CommonConfirmTransactionRequest implements Serializable {
    private String referenceCode;

    private String otpCode;
}
