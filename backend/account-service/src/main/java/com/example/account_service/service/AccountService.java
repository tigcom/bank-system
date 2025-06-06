package com.example.account_service.service;

import com.example.account_service.dto.request.PaymentCreateDTO;
import com.example.account_service.dto.request.PaymentConfirmOtpDTO;
import com.example.account_service.dto.request.SavingCreateDTO;
import com.example.account_service.dto.response.AccountCreateReponse;
import com.example.account_service.dto.response.PaymentRequestResponse;
import com.example.common_service.dto.response.AccountPaymentResponse;
import com.example.common_service.dto.response.AccountSummaryDTO;

import java.util.List;

public interface AccountService {

    PaymentRequestResponse createPaymentRequest(String cifcode);
    
    AccountCreateReponse confirmOtpAndCreatePayment(PaymentConfirmOtpDTO paymentConfirmOtpDTO);
    
    void resendPaymentOtp(String tempRequestKey);

    AccountCreateReponse createPayment();

    List<AccountSummaryDTO> getAllAccountsbyCifCode();

    List<AccountPaymentResponse> getAllPaymentAccountsbyCifCode();
}
