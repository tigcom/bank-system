package com.example.loan_service.dto.response;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InfoIncomeResponseDto {
    private Long infoId;
    private String accountNumber;
    private String bankName;
    private BigDecimal declaredIncome;
}
