package com.example.account_service.controller;

import com.example.account_service.dto.request.CreditRequestConfirmDTO;
import com.example.account_service.dto.request.CreditRequestCreateDTO;
import com.example.account_service.dto.request.CreditSensitiveConfirmDTO;
import com.example.account_service.dto.request.PaymentCreateDTO;
import com.example.account_service.dto.request.SavingCreateDTO;
import com.example.account_service.dto.response.AccountCreateReponse;
import com.example.account_service.dto.response.ApiResponseWrapper;
import com.example.account_service.dto.response.CreditRequestReponse;
import com.example.account_service.dto.response.CreditSensitiveReponse;
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
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PostMapping("/confirm-otp-credit")
    public ApiResponseWrapper<CreditRequestReponse> createCreditRequest(@RequestBody CreditRequestConfirmDTO creditRequestConfirmDTO) {
        CreditRequestReponse reponse = creditRequestService.confirmOTPAndCreateAccount(creditRequestConfirmDTO);
        return ApiResponseWrapper.<CreditRequestReponse>builder()
                .status(HttpStatus.CREATED.value())
                .message(messageUtils.getMessage("OTP.validated"))
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
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("admin/approve-credit-request/{id}")
    public ApiResponseWrapper<AccountCreateReponse> approveRequest(@PathVariable String id) {
        AccountCreateReponse reponse = creditRequestService.approveCreditRequest(id);
        return ApiResponseWrapper.<AccountCreateReponse>builder()
                .status(HttpStatus.CREATED.value())
                .message(messageUtils.getMessage("account.credit.approved"))
                .data(reponse)
                .build();
    }
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("admin/reject-credit-request/{id}")
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
    @PostMapping("/resend-otp-credit/{tempRequestKey}")
    public ApiResponseWrapper<String> resendPaymentOtp(@PathVariable String tempRequestKey) {
        creditRequestService.resendCreditOtp(tempRequestKey);
        return ApiResponseWrapper.<String>builder()
                .status(HttpStatus.OK.value())
                .message("OTP đã được gửi lại thành công.")
                .data("OTP resent to user email.")
                .build();
    }
    @PostMapping("/credit/getSensitiveInfo/{accountNumber}")
    public ApiResponseWrapper<CreditSensitiveReponse> getCreditSensitiveInfo(@PathVariable String accountNumber)
    {
        CreditSensitiveReponse reponse= creditRequestService.getCreditSensitiveResponse(accountNumber);
        ApiResponseWrapper<CreditSensitiveReponse> response = new ApiResponseWrapper<>(
                HttpStatus.OK.value(),
                messageUtils.getMessage("account.credit-request.list"),
                reponse
        );
        return response;
    }
    @Operation(
            summary = "Send OTP for Credit Sensitive Information",
            description = "Send OTP to customer's email for accessing sensitive credit card information"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OTP sent successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid account number"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/send-otp/credit/getSensitiveInfo/{accountNumber}")
    public ApiResponseWrapper<String> sendOTPForSensitiveInfo(@PathVariable String accountNumber) {
        String tempRequestKey = creditRequestService.sendOTPForSensitiveInfo(accountNumber);
        return ApiResponseWrapper.<String>builder()
                .status(HttpStatus.OK.value())
                .message("OTP đã được gửi đến email của bạn để truy cập thông tin thẻ.")
                .data(tempRequestKey)
                .build();
    }

    @Operation(
            summary = "Confirm OTP and Get Credit Sensitive Information",
            description = "Confirm OTP and retrieve sensitive credit card information including card number, expiry date, and holder name"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OTP validated and sensitive info retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid OTP or request data"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/confirm-otp/credit/getSensitiveInfo")
    public ApiResponseWrapper<CreditSensitiveReponse> confirmOTPAndGetSensitiveInfo(@RequestBody @Valid CreditSensitiveConfirmDTO confirmDTO) {
        CreditSensitiveReponse response = creditRequestService.confirmOTPAndGetSensitiveInfo(confirmDTO);
        return ApiResponseWrapper.<CreditSensitiveReponse>builder()
                .status(HttpStatus.OK.value())
                .message("Xác thực OTP thành công. Thông tin thẻ đã được truy xuất.")
                .data(response)
                .build();
    }

    @Operation(
            summary = "Resend OTP for Credit Sensitive Information",
            description = "Resend OTP to customer's email for accessing sensitive credit card information"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OTP resent successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired request"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/resend-otp/credit/getSensitiveInfo/{tempRequestKey}")
    public ApiResponseWrapper<String> resendSensitiveInfoOtp(@PathVariable String tempRequestKey) {
        creditRequestService.resendSensitiveInfoOtp(tempRequestKey);
        return ApiResponseWrapper.<String>builder()
                .status(HttpStatus.OK.value())
                .message("OTP đã được gửi lại thành công.")
                .data("OTP resent to user email for sensitive info access.")
                .build();
    }
}