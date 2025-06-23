package com.example.common_service.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Builder
@Data
public class WithdrawSavingDTO  implements Serializable {
    private static final long serialVersionUID = 1L;
    private BigDecimal withdrawAmount;
    private String withdrawType;
    private String destinationAccountNumber;
    private String savingsAccountNumber;
    private BigDecimal amountOriginal;
}