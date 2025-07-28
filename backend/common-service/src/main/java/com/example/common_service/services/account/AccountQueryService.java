package com.example.common_service.services.account;

import com.example.common_service.dto.AccountDTO;
import com.example.common_service.dto.CustomerDTO;
import com.example.common_service.dto.response.AccountPaymentResponse;
import com.example.common_service.dto.response.BalanceResponse;

import java.util.List;


public interface AccountQueryService {
    AccountDTO getAccountByAccountNumber(String accountNumber);
    boolean existsAccountByAccountNumberAndCifCode(String accountNumber, String cifCode);
    List<AccountPaymentResponse> getAllAccountPaymentForCurrentCustomer();
    CustomerDTO getCustomerByAccountNumber(String accountNumber);
}
