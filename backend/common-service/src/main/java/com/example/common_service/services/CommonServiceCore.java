package com.example.common_service.services;

import com.example.common_service.dto.CorePaymentAccountDTO;
import com.example.common_service.dto.coreSavingAccountDTO;

public interface CommonServiceCore {
    void createCoreAccountPayment(CorePaymentAccountDTO dto);
    void createCoreAccountSaving(coreSavingAccountDTO dto);
}
