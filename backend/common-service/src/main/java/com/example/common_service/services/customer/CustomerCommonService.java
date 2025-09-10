package com.example.common_service.services.customer;

import com.example.common_service.dto.AccountDTO;
import com.example.common_service.dto.PaymentCreateDTO;
import com.example.common_service.dto.response.PaymentRequestResponse;
import com.example.common_service.dto.response.AccountPaymentResponse;

import java.util.List;

public interface CustomerCommonService {
    List<AccountDTO> getAccountsByCifCode(String cifCode);

    List<AccountPaymentResponse> getAllPaymentAccountsbyUserId(String userId);

    PaymentRequestResponse createPaymentInit(PaymentCreateDTO paymentRequest);

}


