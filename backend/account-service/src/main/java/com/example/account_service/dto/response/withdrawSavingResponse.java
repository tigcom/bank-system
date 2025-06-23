package com.example.account_service.dto.response;

import com.example.common_service.constant.SavingsRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class withdrawSavingResponse {

    private String id;
    private BigDecimal withdrawAmount;
    private String withdrawType;
    private String destinationAccountNumber;
    private String savingsAccountNumber;
    private BigDecimal amountOriginal;
}
