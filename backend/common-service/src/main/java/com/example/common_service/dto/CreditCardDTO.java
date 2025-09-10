package com.example.common_service.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
public class CreditCardDTO implements  Serializable {
    private String cardID;
    private String typeName;
    private BigDecimal defaultCreditLimit  = BigDecimal.ZERO;
    private BigDecimal interestRate  = BigDecimal.ZERO;
    private BigDecimal annualFee  = BigDecimal.ZERO;
    private BigDecimal minimumIncome = BigDecimal.ZERO;
    private String imgURL;
}
