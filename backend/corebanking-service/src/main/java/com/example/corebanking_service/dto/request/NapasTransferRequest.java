package com.example.corebanking_service.dto.request;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class NapasTransferRequest {
    private String toAccountNumber;
    private BigDecimal amount;
    private String description;
    private String bankCode;
}
