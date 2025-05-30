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
public class coreSavingAccountDTO implements Serializable {
    private String cifCode;
    private Integer term;
    private BigDecimal initialDeposit;
    private final AccountType accountType = AccountType.SAVING;
    private String accountNumber;
}
