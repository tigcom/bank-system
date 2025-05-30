package com.example.common_service.dto.response;

import com.example.common_service.constant.AccountStatus;
import com.example.common_service.constant.AccountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountPaymentResponse {
    private String accountNumber;
    private String cifCode;
    private AccountType accountType;
    private BigDecimal balance;
    private AccountStatus status;
    private LocalDate openedDate;
}