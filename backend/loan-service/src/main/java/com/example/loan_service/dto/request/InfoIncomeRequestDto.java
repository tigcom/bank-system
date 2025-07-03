package com.example.loan_service.dto.request;

import com.example.loan_service.models.LoanStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

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
