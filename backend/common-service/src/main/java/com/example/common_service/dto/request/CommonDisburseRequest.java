package com.example.common_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CommonDisburseRequest {

    private String toAccountNumber;

    private BigDecimal amount;

    private String description;

    private String currency;
}
