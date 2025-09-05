package com.example.account_service.controller;
import com.example.account_service.dto.response.CreditRequestReponse;

import com.example.account_service.dto.request.PaymentConfirmOtpDTO;
import com.example.account_service.dto.request.PaymentCreateDTO;
import com.example.account_service.dto.response.*;
import com.example.account_service.dto.response.ApiResponseWrapper;
import com.example.account_service.service.AccountService;
import com.example.account_service.utils.MessageUtils;
import com.example.common_service.dto.CreditCardDTO;
import com.example.common_service.dto.CustomerDTO;
import com.example.common_service.dto.response.AccountPaymentResponse;
import com.example.common_service.dto.response.AccountSummaryDTO;
import com.example.common_service.dto.response.CreditAccountResponse;
import com.example.common_service.dto.response.SavingAccountResponse;
import com.example.common_service.dto.response.LoanAccountResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AccountController {
    private final AccountService accountService;
    private final MessageUtils messageUtils;

    @Operation(
            summary = "Create Payment Account Request",
            description = "Creates a payment account request. If customer has no existing payment accounts, creates directly. Otherwise requires OTP verification."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Payment account request created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/create-payment-request/{cifCode}")
    public ApiResponseWrapper<PaymentRequestResponse> createPaymentRequest(@PathVariable String cifCode) {
        PaymentRequestResponse response = accountService.createPaymentRequest(cifCode);
        String message = response.getStatus() == PaymentRequestResponse.PaymentRequestStatus.APPROVED ?
                "Tài khoản thanh toán đã được tạo thành công" :
                "OTP đã được gửi đến email của bạn. Vui lòng xác thực để hoàn tất tạo tài khoản.";

        return ApiResponseWrapper.<PaymentRequestResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message(message)
                .data(response)
                .build();
    }

    @PostMapping("/confirm-otp-payment")
    public ApiResponseWrapper<AccountCreateReponse> confirmOtpAndCreatePayment(@Valid @RequestBody PaymentConfirmOtpDTO paymentConfirmOtpDTO) {
        AccountCreateReponse response = accountService.confirmOtpAndCreatePayment(paymentConfirmOtpDTO);
        return ApiResponseWrapper.<AccountCreateReponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Xác thực OTP thành công! Tài khoản thanh toán đã được tạo.")
                .data(response)
                .build();
    }

    @PostMapping("/resend-payment-otp/{tempRequestKey}")
    public ApiResponseWrapper<String> resendPaymentOtp(@PathVariable String tempRequestKey) {
        accountService.resendPaymentOtp(tempRequestKey);
        return ApiResponseWrapper.<String>builder()
                .status(HttpStatus.OK.value())
                .message("OTP đã được gửi lại thành công.")
                .data("OTP resent to user email.")
                .build();
    }
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
    @GetMapping("/getAllSavingAccount")
    public ApiResponseWrapper<List<SavingAccountResponse>>  getAllSavingAccountByCurrentCustomer() {
        List<SavingAccountResponse> list  = accountService.getAllSavingAccountbyCifCode();
        ApiResponseWrapper<List<SavingAccountResponse>> response = new ApiResponseWrapper<>(
                HttpStatus.OK.value(),
                messageUtils.getMessage("account.get-all.success"),
                list
        );
        return response;
    }
    @GetMapping("/getAllCreditAccount")
    public ApiResponseWrapper<List<CreditAccountResponse>>  getAllCreditAccountisActiveByCurrentCustomer() {
        List<CreditAccountResponse> list  = accountService.getAllCreditAccountbyCifCode();
        ApiResponseWrapper<List<CreditAccountResponse>> response = new ApiResponseWrapper<>(
                HttpStatus.OK.value(),
                messageUtils.getMessage("account.get-all.success"),
                list
        );
        return response;
    }
    @GetMapping("/getAllCreditAccount-anyway")
    public ApiResponseWrapper<List<CreditAccountResponse>>  getAllCreditAccountByCurrentCustomer() {
        List<CreditAccountResponse> list  = accountService.getAllCreditAccountNonbyCifCode();
        ApiResponseWrapper<List<CreditAccountResponse>> response = new ApiResponseWrapper<>(
                HttpStatus.OK.value(),
                messageUtils.getMessage("account.get-all.success"),
                list
        );
        return response;
    }

    @GetMapping("/getAllLoanAccounts")
    public ApiResponseWrapper<List<LoanAccountResponse>> getAllLoanAccountsByCurrentCustomer() {
        log.info("GET_ALL_LOAN_ACCOUNTS_START - CifCode: ");

        try {
            List<LoanAccountResponse> list = accountService.getAllLoanAccountsByCifCode();

            if (list == null || list.isEmpty()) {
                log.warn("GET_ALL_LOAN_ACCOUNTS_EMPTY - CifCode: Reason: NO_LOAN_ACCOUNT_FOUND");
            } else {
                log.info("GET_ALL_LOAN_ACCOUNTS_SUCCESS - CifCode: TotalAccounts: {}", list.size());
            }

            return new ApiResponseWrapper<>(
                    HttpStatus.OK.value(),
                    messageUtils.getMessage("account.get-all.success"),
                    list
            );
        } catch (Exception e) {
            log.error("GET_ALL_LOAN_ACCOUNTS_ERROR - , Error: {}", e.getMessage(), e);
            throw e;
        }
    }


    @GetMapping("/loan/{accountNumber}")
    public ApiResponseWrapper<LoanAccountResponse> getLoanAccountByNumber(@PathVariable String accountNumber) {
        LoanAccountResponse data = accountService.getLoanAccountByNumber(accountNumber);
        return new ApiResponseWrapper<>(
                HttpStatus.OK.value(),
                messageUtils.getMessage("account.getAccount-loan.success"),
                data
        );
    }

    @PutMapping("/loan/{accountNumber}/debt")
    public ApiResponseWrapper<String> updateLoanOutstandingDebt(@PathVariable String accountNumber,
                                                                @RequestParam("outstandingDebt") java.math.BigDecimal outstandingDebt) {
        accountService.updateLoanOutstandingDebt(accountNumber, outstandingDebt);
        return new ApiResponseWrapper<>(HttpStatus.OK.value(), "Cập nhật dư nợ thành công", "OK");
    }

    @PutMapping("/loan/{accountNumber}/close")
    public ApiResponseWrapper<String> closeLoanAccount(@PathVariable String accountNumber) {
        accountService.closeLoanAccount(accountNumber);
        return new ApiResponseWrapper<>(HttpStatus.OK.value(), "Đóng tài khoản vay thành công", "OK");
    }

    @GetMapping("/getAccountPaymentByID/{id}")
    public ApiResponseWrapper<AccountPaymentResponse> getAccountPaymentByID(@PathVariable String id) {
        AccountPaymentResponse accountPaymentResponse = accountService.getAccountPaymentbyID(id);
        ApiResponseWrapper<AccountPaymentResponse> response = new ApiResponseWrapper<>(
                HttpStatus.OK.value(),
                messageUtils.getMessage("account.getAccount-payment.success"),
                accountPaymentResponse
        );
        return response;
    }
    @PostMapping("/testAuth")
    public ResponseEntity<String> testAuth(@AuthenticationPrincipal Jwt jwt) {
        String token = ((JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication()).getToken().getTokenValue();
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
    @GetMapping("/getAllCreditCard")
    public ApiResponseWrapper<List<CreditCardDTO>> getAllCreditCardByCurrentCustomer() {
        List<CreditCardDTO> list = accountService.getAllCreditCard();
        ApiResponseWrapper<List<CreditCardDTO>> response = new ApiResponseWrapper<>(
                HttpStatus.OK.value(),
                messageUtils.getMessage("account.getAll-Credit-card.success"),
                list
        );
        return response;
    }
    @GetMapping("/check-cic")
    public ResponseEntity<CicResponse> checkCIC(@RequestParam String idNumber) {
        return ResponseEntity.ok(accountService.checkCIC(idNumber));
    }
    @PostMapping("/api/v1/create-initial-payment-account")
    public ApiResponseWrapper<PaymentRequestResponse> createPaymentInnit(@RequestBody PaymentCreateDTO paymentRequest) {
        PaymentRequestResponse Paymentresponse = accountService.createPaymentInit(paymentRequest);
        ApiResponseWrapper<PaymentRequestResponse> response = new ApiResponseWrapper<>(
                HttpStatus.OK.value(),
                messageUtils.getMessage("account.payment.createSuccess"),
               Paymentresponse
        );
        return response;
    }
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("admin/get-all-credit-crequest")
    public ApiResponseWrapper<List<CreditRequestReponse>> getAllCreditRequesstPending() {
        List<CreditRequestReponse> list = accountService.getAllCreditRequestPending();
        ApiResponseWrapper<List<CreditRequestReponse>> response = new ApiResponseWrapper<>(
                HttpStatus.OK.value(),
                messageUtils.getMessage("account.getAll-Credit-request.pending"),
                list
        );
        return response;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("admin/get-all-credit-crequest-paginated")
    public ApiResponseWrapper<Page<CreditRequestReponse>> getAllCreditRequesstPendingPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() : 
                Sort.by(sortBy).descending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<CreditRequestReponse> resultPage = accountService.getAllCreditRequestPendingPaginated(pageable);
        
        ApiResponseWrapper<Page<CreditRequestReponse>> response = new ApiResponseWrapper<>(
                HttpStatus.OK.value(),
                messageUtils.getMessage("account.getAll-Credit-request.pending"),
                resultPage
        );
        return response;
    }

}
