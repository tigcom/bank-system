package com.example.common_service.dto;

import com.example.common_service.constant.AccountStatus;
import com.example.common_service.constant.AccountType;
import com.example.common_service.services.CoreAccountBaseDTO;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
public class coreCreditAccountDTO implements  Serializable {
    private String cifCode;
    private BigDecimal creditLimit;
    private BigDecimal interestRate;
    private final AccountType accountType = AccountType.CREDIT;
    private String accountNumber;
}
