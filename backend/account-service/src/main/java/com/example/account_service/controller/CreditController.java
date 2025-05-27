package com.example.account_service.controller;

import com.example.account_service.dto.request.CreditRequestCreateDTO;
import com.example.account_service.dto.request.PaymentCreateDTO;
import com.example.account_service.dto.request.SavingCreateDTO;
import com.example.account_service.dto.response.AccountCreateReponse;
import com.example.account_service.dto.response.ApiResponseWrapper;
import com.example.account_service.dto.response.CreditRequestReponse;
import com.example.account_service.service.AccountService;
import com.example.account_service.service.CreditRequestService;
import com.example.account_service.utils.MessageUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CreditController {

    private final AccountService accountService;
    private final CreditRequestService  creditRequestService;
    private final MessageUtils messageUtils;
    @PostMapping("/create-credit-request")
    public ApiResponseWrapper<CreditRequestReponse> createCreditRequest(@RequestBody  CreditRequestCreateDTO creditRequestCreateDTO) {
        CreditRequestReponse reponse = creditRequestService.createCreditRequest(creditRequestCreateDTO);
        return ApiResponseWrapper.<CreditRequestReponse>builder()
                .status(HttpStatus.CREATED.value())
                .message(messageUtils.getMessage("account.credit-request.created"))
                .data(reponse)
                .build();
    }
    @PutMapping("/approve/{id}")
    public ApiResponseWrapper<AccountCreateReponse> approveRequest(@PathVariable String id) {
        AccountCreateReponse reponse = creditRequestService.approveCreditRequest(id);
        return ApiResponseWrapper.<AccountCreateReponse>builder()
                .status(HttpStatus.CREATED.value())
                .message(messageUtils.getMessage("account.credit.approved"))
                .data(reponse)
                .build();
    }
    @GetMapping("/get-all-credit-request")
    public ApiResponseWrapper<List<CreditRequestReponse>> getAllCreditRequest() {
        List<CreditRequestReponse> list = creditRequestService.getAllCreditRequest();
        ApiResponseWrapper<List<CreditRequestReponse>> response = new ApiResponseWrapper<>(
                HttpStatus.OK.value(),
                messageUtils.getMessage("account.credit-request.list"),
                list
        );
        return response;
    }

}
