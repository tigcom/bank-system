package com.example.corebanking_service.service;

import com.example.common_service.dto.CartTypeDTO;
import com.example.common_service.dto.CorePaymentAccountDTO;
import com.example.common_service.dto.coreCreditAccountDTO;
import com.example.common_service.dto.coreSavingAccountDTO;
import com.example.common_service.dto.response.AccountSummaryDTO;

import java.util.List;

public interface CoreAccountService {
    void createCoreAccountPayment(CorePaymentAccountDTO dto);
    void createCoreAccountSaving(coreSavingAccountDTO dto);

    CartTypeDTO getCartTypebyID(String id);

    void createCoreAccountCredit(coreCreditAccountDTO coreCreditAccountDTO);

    List<AccountSummaryDTO> getAllAccountsByCif(String id);
}
