package com.example.common_service.dto;

import com.example.common_service.constant.CustomerStatus;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class CartTypeDTO  implements  Serializable {

    private String typeName;
    private BigDecimal defaultCreditLimit  = BigDecimal.ZERO; ;
    private BigDecimal interestRate  = BigDecimal.ZERO; ;
    private BigDecimal annualFee  = BigDecimal.ZERO; ;
    private BigDecimal minimumIncome = BigDecimal.ZERO; ;
}
