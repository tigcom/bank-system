package com.opening.banking.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InfoIncomeRequestDto {
    private Long infoId;
    private String accountNumber;
    private String bankName;
    private BigDecimal declaredIncome;
}
