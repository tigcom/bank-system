package com.example.account_service.controller;

import com.example.account_service.dto.request.PaymentCreateDTO;
import com.example.account_service.dto.request.SavingCreateDTO;
import com.example.account_service.dto.response.AccountCreateReponse;
import com.example.account_service.dto.response.ApiResponseWrapper;
import com.example.account_service.service.AccountService;
import com.example.account_service.utils.MessageUtils;
import com.example.common_service.dto.response.AccountSummaryDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @Operation(
            summary = "Create Payment Account",
            description = "Creates a new payment account based on the provided details."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Payment account created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/createPayment")
    public ApiResponseWrapper<AccountCreateReponse> createPayment() {
                AccountCreateReponse accountCreateReponse= accountService.createPayment();
                return ApiResponseWrapper.<AccountCreateReponse>builder()
                        .status(HttpStatus.CREATED.value())
                        .message("account.payment.createSuccess")
                        .data(accountCreateReponse)
                        .build();
    }
    @Operation(
            summary = "Create Saving Account",
            description = "Creates a new saving account with the provided customer and saving account details."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Saving account created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/createSaving")
    public ApiResponseWrapper<AccountCreateReponse> createSaving(@Valid @RequestBody SavingCreateDTO savingCreateDTO) {
        AccountCreateReponse accountCreateReponse= accountService.createSaving(savingCreateDTO);
        return ApiResponseWrapper.<AccountCreateReponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("account.saving.createSuccess")
                .data(accountCreateReponse)
                .build();
    }
    @Operation(
            summary = "Get All Accounts",
            description = "Retrieve all accounts associated with the currently authenticated customer."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of accounts"),
            @ApiResponse(responseCode = "401", description = "Unauthorized access"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/getALlAccount")
    public ApiResponseWrapper<List<AccountSummaryDTO>> getALlAccountByCurrentCustomer() {
        List<AccountSummaryDTO> accountResponses = accountService.getAllAccountsbyCifCode();
        ApiResponseWrapper<List<AccountSummaryDTO>> response = new ApiResponseWrapper<>(
                HttpStatus.OK.value(),
                messageUtils.getMessage("account.get-all.success"),
                accountResponses
        );
        return response;
    }
}
