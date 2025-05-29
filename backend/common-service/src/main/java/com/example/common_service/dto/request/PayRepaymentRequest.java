package com.example.common_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
public class PayRepaymentRequest implements Serializable {
    private String fromAccountNumber;

    private BigDecimal amount;

    private String currency;

    private String description;
}
