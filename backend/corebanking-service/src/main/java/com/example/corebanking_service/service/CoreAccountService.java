package com.example.corebanking_service.service;

import com.example.common_service.dto.*;
import com.example.common_service.dto.request.SavingUpdateRequest;
import com.example.common_service.dto.response.*;

public interface CoreAccountService {
    void createCoreAccount(CoreAccountRequest dto);
    AccountSavingUpdateResponse updateBalanceSaving(String accountNumber, SavingUpdateRequest request);

    BalanceResponse getBalanceByAccountNumber(String accountNumber);

    void updateStatus(CoreAccountUpdateStatusRequest statusRequest);
}
