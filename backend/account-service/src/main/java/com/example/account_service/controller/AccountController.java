package com.example.account_service.controller;

import com.example.account_service.dto.request.PaymentCreateDTO;
import com.example.account_service.dto.request.SavingCreateDTO;
import com.example.account_service.dto.response.AccountCreateReponse;
import com.example.account_service.dto.response.ApiResponseWrapper;
import com.example.account_service.service.AccountService;
import com.example.account_service.utils.MessageUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final MessageUtils messageUtils;

    @PostMapping("/createPayment")
    public ApiResponseWrapper<AccountCreateReponse> createPayment(@Valid @RequestBody PaymentCreateDTO paymentCreateDTO ) {
                AccountCreateReponse accountCreateReponse= accountService.createPayment(paymentCreateDTO);
                return ApiResponseWrapper.<AccountCreateReponse>builder()
                        .status(HttpStatus.CREATED.value())
                        .message("account.payment.createSuccess")
                        .data(accountCreateReponse)
                        .build();
    }
    @PostMapping("/createSaving")
    public ApiResponseWrapper<AccountCreateReponse> createSaving(@Valid @RequestBody SavingCreateDTO savingCreateDTO) {
        AccountCreateReponse accountCreateReponse= accountService.createSaving(savingCreateDTO);
        return ApiResponseWrapper.<AccountCreateReponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("account.saving.createSuccess")
                .data(accountCreateReponse)
                .build();
    }
    @GetMapping("/getALlAcount")
    public ApiResponseWrapper<List<AccountCreateReponse>> getALlAcount() {
        List<AccountCreateReponse> accountResponses = accountService.getAllAccounts();
        ApiResponseWrapper<List<AccountCreateReponse>> response = new ApiResponseWrapper<>(
                HttpStatus.OK.value(),
                messageUtils.getMessage("account.get-all.success"),
                accountResponses
        );
        return response;
    }
}
