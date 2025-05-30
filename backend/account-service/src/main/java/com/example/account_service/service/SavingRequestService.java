package com.example.account_service.service;

import com.example.account_service.dto.request.ConfirmRequestDTO;
import com.example.account_service.dto.request.CreditRequestCreateDTO;
import com.example.account_service.dto.request.SavingRequestCreateDTO;
import com.example.account_service.dto.response.AccountCreateReponse;
import com.example.account_service.dto.response.CreditRequestReponse;
import com.example.account_service.dto.response.SavingsRequestResponse;
import com.example.common_service.dto.response.CoreTermDTO;

import java.util.List;

public interface SavingRequestService {

    SavingsRequestResponse CreateSavingRequest(SavingRequestCreateDTO savingRequestCreateDTO);

    void sendOTP(String id);

    SavingsRequestResponse confirmOTPandSave(ConfirmRequestDTO confirmRequestDTO);

    List<CoreTermDTO> getAllTerm();
}
