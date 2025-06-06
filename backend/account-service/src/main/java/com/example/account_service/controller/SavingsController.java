package com.example.account_service.controller;

import com.example.account_service.dto.request.ConfirmRequestDTO;
import com.example.account_service.dto.request.CreditRequestCreateDTO;
import com.example.account_service.dto.request.SavingRequestCreateDTO;
import com.example.account_service.dto.response.ApiResponseWrapper;
import com.example.account_service.dto.response.CreditRequestReponse;
import com.example.account_service.dto.response.SavingsRequestResponse;
import com.example.account_service.service.AccountService;
import com.example.account_service.service.CreditRequestService;
import com.example.account_service.service.SavingRequestService;
import com.example.account_service.utils.MessageUtils;
import com.example.common_service.dto.response.CoreTermDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Savings Request", description = "APIs for creating and managing savings accounts")
@RestController
@RequiredArgsConstructor
public class SavingsController {
    private final AccountService accountService;
    private final CreditRequestService creditRequestService;
    private final MessageUtils messageUtils;
    private final SavingRequestService savingRequestService;
    
    @PostMapping("/create-savings-request")
    public ApiResponseWrapper<SavingsRequestResponse> createSavingsRequest(@RequestBody SavingRequestCreateDTO savingRequestCreateDTO) {
        SavingsRequestResponse response = savingRequestService.CreateSavingRequest(savingRequestCreateDTO);
        return ApiResponseWrapper.<SavingsRequestResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message(messageUtils.getMessage("account.saving-request-created"))
                .data(response)
                .build();
    }
    
    @PostMapping("/resend-otp/{tempRequestKey}")
    public ApiResponseWrapper<String> resendOTP(@PathVariable String tempRequestKey) {
        savingRequestService.resendOTP(tempRequestKey);
        return ApiResponseWrapper.<String>builder()
                .status(HttpStatus.OK.value())
                .message("OTP đã được gửi lại thành công.")
                .data("OTP resent to user email.")
                .build();
    }
    
        @PostMapping("/confirm-otp-and-create-account")
    public ApiResponseWrapper<SavingsRequestResponse> confirmOTPAndCreateAccount(@RequestBody ConfirmRequestDTO confirmRequestDTO) {
        SavingsRequestResponse savingsRequestResponse = savingRequestService.confirmOTPAndCreateSavingAccount(confirmRequestDTO);
        return ApiResponseWrapper.<SavingsRequestResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message(messageUtils.getMessage("account.saving.createSuccess"))
                .data(savingsRequestResponse)
                .build();
    }
    
    @GetMapping("/get-all-term")
    public ApiResponseWrapper<List<CoreTermDTO>> getAllTermIsActive() {
        List<CoreTermDTO> list = savingRequestService.getAllTerm();
        return ApiResponseWrapper.<List<CoreTermDTO>>builder()
                .status(HttpStatus.OK.value())
                .message(messageUtils.getMessage("core-term.get-all.success"))
                .data(list)
                .build();
    }
}
