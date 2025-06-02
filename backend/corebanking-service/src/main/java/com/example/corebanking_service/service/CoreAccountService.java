package com.example.corebanking_service.service;

import com.example.common_service.dto.CartTypeDTO;
import com.example.common_service.dto.CorePaymentAccountDTO;
import com.example.common_service.dto.coreCreditAccountDTO;
import com.example.common_service.dto.CoreSavingAccountDTO;
import com.example.common_service.dto.response.AccountPaymentResponse;
import com.example.common_service.dto.response.AccountSummaryDTO;
import com.example.common_service.dto.response.CoreTermDTO;

import java.util.List;

public interface CoreAccountService {
    void createCoreAccountPayment(CorePaymentAccountDTO dto);
    void createCoreAccountSaving(CoreSavingAccountDTO dto);

    CartTypeDTO getCartTypebyID(String id);

    void createCoreAccountCredit(coreCreditAccountDTO coreCreditAccountDTO);

    List<AccountSummaryDTO> getAllAccountsByCif(String id);

    List<AccountPaymentResponse> getAllPaymentAccountsByCif(String id);

    List<CoreTermDTO> getAllCoreTerm();
}
