package com.example.account_service.service;

import com.example.account_service.dto.request.ConfirmRequestDTO;
import com.example.account_service.dto.request.CreditRequestCreateDTO;
import com.example.account_service.dto.request.SavingRequestCreateDTO;
import com.example.account_service.dto.response.AccountCreateReponse;
import com.example.account_service.dto.response.CreditRequestReponse;
import com.example.account_service.dto.response.SavingsRequestResponse;
import com.example.account_service.dto.response.withdrawSavingResponse;
import com.example.common_service.dto.WithdrawSavingDTO;
import com.example.common_service.dto.response.CoreTermDTO;
import jakarta.validation.Valid;

import java.util.List;

public interface SavingRequestService {

    SavingsRequestResponse CreateSavingRequest(SavingRequestCreateDTO savingRequestCreateDTO);

    void resendOTP(String tempRequestKey);

    SavingsRequestResponse confirmOTPAndCreateSavingAccount(ConfirmRequestDTO confirmRequestDTO);

    List<CoreTermDTO> getAllTerm();

    withdrawSavingResponse createWithDrawRequest( WithdrawSavingDTO request);

    withdrawSavingResponse confirmOTPAndProcessWithdraw(ConfirmRequestDTO confirmRequestDTO);
}
