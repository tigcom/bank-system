package com.example.account_service.service;

import com.example.account_service.dto.request.PaymentCreateDTO;
import com.example.account_service.dto.request.SavingCreateDTO;
import com.example.account_service.dto.response.AccountCreateReponse;

import java.util.List;

public interface AccountService {

    AccountCreateReponse createPayment(PaymentCreateDTO paymentCreateDTO);

    AccountCreateReponse createSaving(SavingCreateDTO savingCreateDTO);

    List<AccountCreateReponse> getAllAccountsbyCifCode();

}
