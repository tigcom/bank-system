package com.example.common_service.services.account;

import com.example.common_service.dto.AccountDTO;

import java.math.BigDecimal;

public interface AccountQueryService {
    AccountDTO getAccountByNumber(String accountNumber);
    BigDecimal getBalance(String accountNumber);
}
