package com.example.corebanking_service.service;
import com.example.common_service.constant.AccountStatus;
import com.example.corebanking_service.entity.CoreAccountNumber;

import java.util.List;

public interface CoreBankingService {
    List<CoreAccountNumber> generateAccountNumber (String bankcode);

    String getAccountNumber (String typeAccount);


}
