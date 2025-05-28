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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Credit Request", description = "APIs for creating and managing credit requests")
@RestController
@RequiredArgsConstructor
public class CreditController {

    private final AccountService accountService;
    private final CreditRequestService  creditRequestService;
    private final MessageUtils messageUtils;
    @Operation(
            summary = "Create a Credit Request",
            description = "Allows a customer to submit a credit card request. The system will validate the request based on customer status."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Credit request created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/create-credit-request")
    public ApiResponseWrapper<CreditRequestReponse> createCreditRequest(@RequestBody  CreditRequestCreateDTO creditRequestCreateDTO) {
        CreditRequestReponse reponse = creditRequestService.createCreditRequest(creditRequestCreateDTO);
        return ApiResponseWrapper.<CreditRequestReponse>builder()
                .status(HttpStatus.CREATED.value())
                .message(messageUtils.getMessage("account.credit-request.created"))
                .data(reponse)
                .build();
    }
    @Operation(
            summary = "Approve Credit Request",
            description = "Approve a credit card request by validating customer age and income, then creates a credit account."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Credit request approved and account created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid credit request or customer data"),
            @ApiResponse(responseCode = "404", description = "Credit request not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/approve/{id}")
    public ApiResponseWrapper<AccountCreateReponse> approveRequest(@PathVariable String id) {
        AccountCreateReponse reponse = creditRequestService.approveCreditRequest(id);
        return ApiResponseWrapper.<AccountCreateReponse>builder()
                .status(HttpStatus.CREATED.value())
                .message(messageUtils.getMessage("account.credit.approved"))
                .data(reponse)
                .build();
    }
    @PutMapping("/reject/{id}")
    public ApiResponseWrapper<CreditRequestReponse> rejectRequest(@PathVariable String id) {
        CreditRequestReponse reponse = creditRequestService.rejectCreditRequest(id);
        return ApiResponseWrapper.<CreditRequestReponse>builder()
                .status(HttpStatus.OK.value())
                .message(messageUtils.getMessage("account.credit.reject"))
                .data(reponse)
                .build();
    }

    @Operation(
            summary = "Get all credit requests",
            description = "Retrieve a list of all credit requests from the system."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of credit requests"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
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
