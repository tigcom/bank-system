package com.example.corebanking_service.service;

import com.example.common_service.dto.*;
import com.example.common_service.dto.request.SavingUpdateRequest;
import com.example.common_service.dto.response.*;
import com.example.corebanking_service.dto.request.LoanRequestDTO;

public interface CoreAccountService {
    void createCoreAccount(CoreAccountRequest dto);
    AccountSavingUpdateResponse updateBalanceSaving(String accountNumber, SavingUpdateRequest request);

    BalanceResponse getBalanceByAccountNumber(String accountNumber);
    void updateCoreAccount(CoreAccountRequest dto);

    void updateStatus(CoreAccountUpdateStatusRequest statusRequest);
}
