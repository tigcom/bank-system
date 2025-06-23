package com.example.account_service.service;

import com.example.account_service.dto.request.PaymentConfirmOtpDTO;
import com.example.account_service.dto.request.PaymentCreateDTO;
import com.example.account_service.dto.request.PaymentRequest;
import com.example.account_service.dto.response.AccountCreateReponse;
import com.example.account_service.dto.response.CicResponse;
import com.example.account_service.dto.response.PaymentRequestResponse;
import com.example.common_service.dto.CreditCardDTO;
import com.example.common_service.dto.response.AccountPaymentResponse;
import com.example.common_service.dto.response.AccountSummaryDTO;
import com.example.common_service.dto.response.CreditAccountResponse;
import com.example.common_service.dto.response.SavingAccountResponse;

import java.util.List;

public interface AccountService {

    PaymentRequestResponse createPaymentRequest(String cifcode);
    
    AccountCreateReponse confirmOtpAndCreatePayment(PaymentConfirmOtpDTO paymentConfirmOtpDTO);
    
    void resendPaymentOtp(String tempRequestKey);


    List<AccountSummaryDTO> getAllAccountsbyCifCode();

    List<AccountPaymentResponse> getAllPaymentAccountsbyCifCode();

    AccountPaymentResponse getAccountPaymentbyID(String id);

    List<SavingAccountResponse> getAllSavingAccountbyCifCode();

    List<CreditAccountResponse> getAllCreditAccountbyCifCode();

    List<CreditCardDTO> getAllCreditCard();

    CicResponse checkCIC(String idNumber);
    PaymentRequestResponse createPaymentInit(PaymentCreateDTO paymentRequest);


}