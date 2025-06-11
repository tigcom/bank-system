package com.example.account_service.controller;

import com.example.account_service.dto.request.SavingCreateDTO;
import com.example.account_service.dto.response.AccountCreateReponse;
import com.example.account_service.dto.response.ApiResponseWrapper;
import com.example.account_service.service.AccountService;
import com.example.account_service.utils.MessageUtils;
import com.example.common_service.dto.CustomerDTO;
import com.example.common_service.dto.response.AccountPaymentResponse;
import com.example.common_service.dto.response.AccountSummaryDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
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
                .message(messageUtils.getMessage("account.payment.createSuccess"))
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
                .message(messageUtils.getMessage("account.saving.createSuccess"))
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
    @GetMapping("/getAllPaymentAccount")
    public ApiResponseWrapper<List<AccountPaymentResponse>> getALlPaymentAccountByCurrentCustomer() {
        List<AccountPaymentResponse> accountResponses = accountService.getAllPaymentAccountsbyCifCode();
        ApiResponseWrapper<List<AccountPaymentResponse>> response = new ApiResponseWrapper<>(
                HttpStatus.OK.value(),
                messageUtils.getMessage("account.get-all.success"),
                accountResponses
        );
        return response;
    }

    @PostMapping("/testAuth")
    public ResponseEntity<String> testAuth(@AuthenticationPrincipal Jwt jwt) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String token = ((JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication()).getToken().getTokenValue();
        System.out.println(token);
        return ResponseEntity.ok("Test auth with service, user: " + token);
    }

    @GetMapping("/get-customer/{accountNumber}")
    public ApiResponseWrapper<CustomerDTO> getCustomerByAccountNumber(@PathVariable String accountNumber){
        return ApiResponseWrapper.<CustomerDTO>builder()
                .message("Thông tin khách hàng")
                .status(HttpStatus.OK.value())
                .data(accountService.getCustomerByAccountNumber(accountNumber))
                .build();
    }

}
