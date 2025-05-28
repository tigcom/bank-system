package com.example.account_service.service;

import com.example.account_service.dto.request.PaymentCreateDTO;
import com.example.account_service.dto.request.SavingCreateDTO;
import com.example.account_service.dto.response.AccountCreateReponse;
import com.example.common_service.dto.response.AccountSummaryDTO;

import java.util.List;

public interface AccountService {

    AccountCreateReponse createPayment();

    AccountCreateReponse createSaving(SavingCreateDTO savingCreateDTO);

    List<AccountSummaryDTO> getAllAccountsbyCifCode();
}
