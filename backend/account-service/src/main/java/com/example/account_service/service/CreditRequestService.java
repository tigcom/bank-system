package com.example.account_service.service;

import com.example.account_service.dto.request.CreditRequestCreateDTO;
import com.example.account_service.dto.request.CreditRequestConfirmDTO;
import com.example.account_service.dto.request.PaymentCreateDTO;
import com.example.account_service.dto.request.SavingCreateDTO;
import com.example.account_service.dto.response.AccountCreateReponse;
import com.example.account_service.dto.response.CreditRequestReponse;

import java.util.List;

public interface CreditRequestService {

    CreditRequestReponse createCreditRequest(CreditRequestCreateDTO creditRequestCreateDTO);

    void sendOTP(String creditRequestId);

    CreditRequestReponse confirmOTPAndCreateAccount(CreditRequestConfirmDTO creditRequestConfirmDTO);

    AccountCreateReponse approveCreditRequest(String id);

    List<CreditRequestReponse> getAllCreditRequest();

    CreditRequestReponse rejectCreditRequest(String id);
}
