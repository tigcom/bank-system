package com.example.common_service.dto;

import com.example.common_service.constant.AccountStatus;
import com.example.common_service.constant.AccountType;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
public class CoreAccountRequest implements Serializable {
    private String accountNumber;
    private String cifCode;
    private AccountType accountType;
    private BigDecimal balance;
    private AccountStatus status;
}
