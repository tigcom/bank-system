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

@Tag(name = "Credit Request", description = "APIs for creating and managing credit requests")
@RestController
@RequiredArgsConstructor
public class SavingsController {
    private final AccountService accountService;
    private final CreditRequestService creditRequestService;
    private final MessageUtils messageUtils;
    private final SavingRequestService savingRequestService;
    @PostMapping("/create-savings-request")
    public ApiResponseWrapper<SavingsRequestResponse> createCreditRequest(@RequestBody SavingRequestCreateDTO savingRequestCreateDTO) {
        SavingsRequestResponse reponse = savingRequestService.CreateSavingRequest(savingRequestCreateDTO);
        return ApiResponseWrapper.<SavingsRequestResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message(messageUtils.getMessage("account.credit-request.created"))
                .data(reponse)
                .build();
    }
    @PostMapping("/Send-otp-to-email/{id}")
    public ApiResponseWrapper<String> SendOTP(@PathVariable String id) {
         savingRequestService.sendOTP(id);
        return ApiResponseWrapper.<String>builder()
                .status(HttpStatus.OK.value())
                .message("OTP đã được gửi thành công.")
                .data("OTP sent to user contact.")
                .build();

    }
    /// api confirm otp va save account saving
    @PostMapping("/confirm-otp")
    public ApiResponseWrapper<SavingsRequestResponse> confirmRequest(@RequestBody ConfirmRequestDTO confirmRequestDTO)
    {
        SavingsRequestResponse savingsRequestResponse = savingRequestService.confirmOTPandSave(confirmRequestDTO);
        return ApiResponseWrapper.<SavingsRequestResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message(messageUtils.getMessage("account.saving-request.confirmed"))
                .data(savingsRequestResponse)
                .build();
    }
    @GetMapping("/get-all-term")
    public ApiResponseWrapper<List<CoreTermDTO>> getAllTermIsActive() {
        List<CoreTermDTO> list = savingRequestService.getAllTerm();
        return ApiResponseWrapper.<List<CoreTermDTO>>builder()
                .status(HttpStatus.CREATED.value())
                .message(messageUtils.getMessage("core-term.get-all.success"))
                .data(list)
                .build();

    }
}
