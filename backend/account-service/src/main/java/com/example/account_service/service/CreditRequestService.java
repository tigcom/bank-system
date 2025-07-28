package com.example.account_service.service;

import com.example.account_service.dto.request.CreditRequestCreateDTO;
import com.example.account_service.dto.request.CreditRequestConfirmDTO;
import com.example.account_service.dto.request.CreditSensitiveConfirmDTO;
import com.example.account_service.dto.request.PaymentCreateDTO;
import com.example.account_service.dto.request.SavingCreateDTO;
import com.example.account_service.dto.response.AccountCreateReponse;
import com.example.account_service.dto.response.CreditRequestReponse;
import com.example.account_service.dto.response.CreditSensitiveReponse;

import java.util.List;

public interface CreditRequestService {

    CreditRequestReponse createCreditRequest(CreditRequestCreateDTO creditRequestCreateDTO);

    void sendOTP(String creditRequestId);

    CreditRequestReponse confirmOTPAndCreateAccount(CreditRequestConfirmDTO creditRequestConfirmDTO);

    AccountCreateReponse approveCreditRequest(String id);

    List<CreditRequestReponse> getAllCreditRequest();

    CreditRequestReponse rejectCreditRequest(String id);

    void resendCreditOtp(String tempRequestKey);

    CreditSensitiveReponse getCreditSensitiveResponse(String accountNumber);

    // New methods for sensitive info OTP
    String sendOTPForSensitiveInfo(String accountNumber);

    CreditSensitiveReponse confirmOTPAndGetSensitiveInfo(CreditSensitiveConfirmDTO confirmDTO);

    void resendSensitiveInfoOtp(String tempRequestKey);
}

