package com.example.common_service.services.account;

import com.example.common_service.dto.AccountDTO;

public interface AccountQueryService {
    AccountDTO getAccountByAccountNumber(String accountNumber);
    boolean existsAccountByAccountNumberAndCifCode(String accountNumber, String cifCode);
}
