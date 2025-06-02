package com.example.common_service.dto;

import com.example.common_service.constant.AccountType;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
public class CoreSavingAccountDTO implements Serializable {
    private String cifCode;
    private Integer term;
    private BigDecimal initialDeposit;
    private final AccountType accountType = AccountType.SAVING;
    private String accountNumber;
}
