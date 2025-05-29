package com.example.common_service.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
public class AccountDTO implements Serializable {
    private String accountNumber;
    private String cifCode;
    private String accountType;
    private BigDecimal balance;
    private String status;
}
