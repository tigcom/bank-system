package com.example.common_service.services;

import com.example.common_service.dto.CartTypeDTO;
import com.example.common_service.dto.CorePaymentAccountDTO;
import com.example.common_service.dto.coreCreditAccountDTO;
import com.example.common_service.dto.CoreSavingAccountDTO;

public interface CommonServiceCore {
    void createCoreAccountPayment(CorePaymentAccountDTO dto);
    void createCoreAccountSaving(CoreSavingAccountDTO dto);

    CartTypeDTO getCartTypebyID(String id);

    void createCoreAccountCredit(coreCreditAccountDTO coreCreditAccountDTO);
}
