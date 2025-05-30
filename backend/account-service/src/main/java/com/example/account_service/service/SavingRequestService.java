package com.example.account_service.service;

import com.example.account_service.dto.request.CreditRequestCreateDTO;
import com.example.account_service.dto.request.SavingRequestCreateDTO;
import com.example.account_service.dto.response.AccountCreateReponse;
import com.example.account_service.dto.response.CreditRequestReponse;
import com.example.account_service.dto.response.SavingsRequestResponse;

import java.util.List;

public interface SavingRequestService {

    SavingsRequestResponse CreateSavingRequest(SavingRequestCreateDTO savingRequestCreateDTO);

    void sendOTP(String id);
}
