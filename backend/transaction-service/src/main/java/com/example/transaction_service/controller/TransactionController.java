package com.example.transaction_service.controller;


import com.example.common_service.dto.CustomerDTO;
import com.example.common_service.dto.response.AccountPaymentResponse;
import com.example.common_service.dto.response.CustomerResponse;
import com.example.transaction_service.dto.TransactionDTO;
import com.example.transaction_service.dto.request.*;
import com.example.transaction_service.dto.response.*;
import com.example.transaction_service.service.ReconciliationService;
import com.example.transaction_service.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.units.qual.A;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Giao dịch", description = "Các API xử lý giao dịch tài khoản như chuyển tiền, nạp tiền, rút tiền, thanh toán hóa đơn...")
public class TransactionController {
    private final TransactionService transactionService;
    private final ReconciliationService reconciliationService;

    @Operation(summary = "Chuyển khoản", description = "Chuyển tiền từ tài khoản nguồn đến tài khoản đích.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Chuyển khoản thành công",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                    {
                        "code": 200,
                        "message": "Chuyển khoản",
                        "result": {
                                "id": "abc123",
                                 "fromAccountNumber": null,
                                 "toAccountNumber": "970452999999999",
                                 "amount": 500000,
                                 "description": "Nạp tiền mặt",
                                  "timestamp": "2025-05-29T10:00:00",
                                  "status": "SUCCESS",
                                  "type": "TRANSFER",
                                  "currency": "VND",
                                  "referenceCode": "TXN-970452999999999-20250529100000abc",
                                  "failedReason": null
                                 }
                               }
                          """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    })
    @PostMapping("/transfer")
    public ApiResponse<TransactionDTO> transfer(@RequestBody @Valid TransferRequest request) {
        return ApiResponse.<TransactionDTO>builder()
                .code(200)
                .message("Chuyển khoản")
                .result(transactionService.transfer(request))
                .build();
    }


    @Operation(summary = "Nạp tiền", description = "Nạp tiền vào tài khoản.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Nạp tiền thành công",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                    {
                        "code": 200,
                        "message": "Nạp tiền vào tài khoản",
                        "result": {
                                "id": "abc123",
                                 "fromAccountNumber": null,
                                 "toAccountNumber": "970452999999999",
                                 "amount": 500000,
                                 "description": "Nạp tiền mặt",
                                  "timestamp": "2025-05-29T10:00:00",
                                  "status": "SUCCESS",
                                  "type": "DEPOSIT",
                                  "currency": "VND",
                                  "referenceCode": "TXN-970452999999999-20250529100000abc",
                                  "failedReason": null
                                 }
                               }
                          """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    })
    @PostMapping("/deposit")
    public ApiResponse<TransactionDTO> deposit(@RequestBody @Valid DepositRequest request) {
        return ApiResponse.<TransactionDTO>builder()
                .code(200)
                .message("Nạp tiền vào tài khoản")
                .result(transactionService.deposit(request))
                .build();
    }

    @Operation(summary = "Rút tiền", description = "Rút tiền từ tài khoản.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Thông tin chi tiết giao dịch",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
            {
                "code": 200,
                "message": "Thông tin chi tiết giao dịch",
                "result": {
                    "id": "e1d62ed3-5a39-4100-89e8-46f6a85a0a80",
                    "fromAccountNumber": "970452999999999",
                    "toAccountNumber": "87654321",
                    "amount": 10000.00,
                    "description": "Rút tiền ",
                    "timestamp": "2025-05-29T10:44:43.419631",
                    "status": "COMPLETED",
                    "type": "WITHDRAW",
                    "currency": "VND",
                    "referenceCode": "TXN-970452999999999-20250529104443f6e662cf",
                    "failedReason": ""
                }
            }
            """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc OTP sai")
    })
    @PostMapping("/withdraw")
    public ApiResponse<TransactionDTO> withdraw(@RequestBody @Valid WithdrawRequest request) {
        return ApiResponse.<TransactionDTO>builder()
                .code(200)
                .message("Rút tiền")
                .result(transactionService.withdraw(request))
                .build();
    }

    @Operation(summary = "Thanh toán hóa đơn", description = "Thanh toán hóa đơn cho tài khoản.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Thông tin chi tiết giao dịch",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
            {
                "code": 200,
                "message": "Thông tin chi tiết giao dịch",
                "result": {
                    "id": "e1d62ed3-5a39-4100-89e8-46f6a85a0a80",
                    "fromAccountNumber": "970452999999999",
                    "toAccountNumber": "87654321",
                    "amount": 10000.00,
                    "description": "Thanh toán hóa đơn ",
                    "timestamp": "2025-05-29T10:44:43.419631",
                    "status": "COMPLETED",
                    "type": "LOAN_PAYMENT",
                    "currency": "VND",
                    "referenceCode": "TXN-970452999999999-20250529104443f6e662cf",
                    "failedReason": ""
                }
            }
            """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc OTP sai")
    })
    @PostMapping("/loan-payment")
    public ApiResponse<TransactionDTO> loanPayment(@RequestBody @Valid LoanPaymentRequest request) {
        return ApiResponse.<TransactionDTO>builder()
                .code(200)
                .message("Thanh toán hóa đơn")
                .result(transactionService.payBillLoan(request))
                .build();
    }

    @Operation(summary = "Giải ngân", description = "Giải ngân khoản vay cho khách hàng.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Thông tin chi tiết giao dịch",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
            {
                "code": 200,
                "message": "Thông tin chi tiết giao dịch",
                "result": {
                    "id": "e1d62ed3-5a39-4100-89e8-46f6a85a0a80",
                    "fromAccountNumber": "970452999999999",
                    "toAccountNumber": "87654321",
                    "amount": 10000.00,
                    "description": "Thanh toán hóa đơn ",
                    "timestamp": "2025-05-29T10:44:43.419631",
                    "status": "COMPLETED",
                    "type": "DISBURSEMENT",
                    "currency": "VND",
                    "referenceCode": "TXN-970452999999999-20250529104443f6e662cf",
                    "failedReason": ""
                }
            }
            """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc OTP sai")
    })
    @PostMapping("/disburse")
    public ApiResponse<TransactionDTO> disburse(@RequestBody @Valid DisburseRequest request) {
        return ApiResponse.<TransactionDTO>builder()
                .code(200)
                .message("Giải ngân khoản vay")
                .result(transactionService.disburse(request ))
                .build();
    }
    @Operation(summary = "Xác nhận giao dịch", description = "Xác nhận OTP để hoàn tất giao dịch.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Thông tin chi tiết giao dịch",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
            {
                "code": 200,
                "message": "Thông tin chi tiết giao dịch",
                "result": {
                    "id": "e1d62ed3-5a39-4100-89e8-46f6a85a0a80",
                    "fromAccountNumber": "970452999999999",
                    "toAccountNumber": "87654321",
                    "amount": 10000.00,
                    "description": "Giải ngân tiền ",
                    "timestamp": "2025-05-29T10:44:43.419631",
                    "status": "COMPLETED",
                    "type": "DISBURSEMENT",
                    "currency": "VND",
                    "referenceCode": "TXN-970452999999999-20250529104443f6e662cf",
                    "failedReason": ""
                }
            }
            """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc OTP sai")
    })
    @PostMapping("/confirm-transaction")
    public ApiResponse<TransactionDTO> confirmTransaction(@RequestBody @Valid ConfirmTransactionRequest request) {
        return ApiResponse.<TransactionDTO>builder()
                .code(200)
                .message("Thông tin chi tiết giao dịch")
                .result(transactionService.confirmTransaction(request))
                .build();
    }
    @Operation(summary = "Gửi lại OTP", description = "Gửi lại mã OTP cho mã giao dịch.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OTP đã được gửi lại thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Giao dịch không hợp lệ")
    })
    @PostMapping("/resend-otp")
    public ApiResponse<String> resendOtp(@RequestBody ResendOtpRequest resendOtpRequest) {
        transactionService.resendOtp(resendOtpRequest);
        return ApiResponse.<String>builder()
                .code(200)
                .message("Mã OTP mới đã được gửi thành công")
                .build();
    }

    @Operation(summary = "Lịch sử giao dịch", description = "Lấy danh sách giao dịch của một tài khoản.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Thông tin chi tiết giao dịch",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
            {
                "code": 200,
                "message": "Thông tin chi tiết giao dịch",
                "result": {
                    "id": "e1d62ed3-5a39-4100-89e8-46f6a85a0a80",
                    "fromAccountNumber": "970452999999999",
                    "toAccountNumber": "87654321",
                    "amount": 10000.00,
                    "description": "Giải ngân tiền ",
                    "timestamp": "2025-05-29T10:44:43.419631",
                    "status": "COMPLETED",
                    "type": "DISBURSEMENT",
                    "currency": "VND",
                    "referenceCode": "TXN-970452999999999-20250529104443f6e662cf",
                    "failedReason": ""
                }
            }
            """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc OTP sai")
    })
    @GetMapping("/account/{accountNumber}")
    public ApiResponse<Page<TransactionDTO>> getTransactionsByAccount(
            @PathVariable String accountNumber,
            @RequestParam int page,
            @RequestParam int size) {
        org.springframework.data.domain.Pageable  pageable =  PageRequest.of(page, size);
        return ApiResponse.<Page<TransactionDTO>>builder()
                .code(200)
                .message("Danh sách giao dịch của tài khoản")
                .result(transactionService.getAccountTransactions(accountNumber, pageable))
                .build();
    }

    @Operation(summary = "Chi tiết giao dịch", description = "Lấy chi tiết giao dịch theo mã tham chiếu.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Thông tin chi tiết giao dịch",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
            {
                "code": 200,
                "message": "Thông tin chi tiết giao dịch",
                "result": {
                    "id": "e1d62ed3-5a39-4100-89e8-46f6a85a0a80",
                    "fromAccountNumber": "970452999999999",
                    "toAccountNumber": "87654321",
                    "amount": 10000.00,
                    "description": "Giải ngân tiền ",
                    "timestamp": "2025-05-29T10:44:43.419631",
                    "status": "COMPLETED",
                    "type": "DISBURSEMENT",
                    "currency": "VND",
                    "referenceCode": "TXN-970452999999999-20250529104443f6e662cf",
                    "failedReason": ""
                }
            }
            """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc OTP sai")
    })

    @GetMapping("/getByReferenceCode/{referenceCode}")
    public ApiResponse<TransactionDTO> getTransactionByReferenceCode(@PathVariable String referenceCode){
        return ApiResponse.<TransactionDTO>builder()
                .code(200)
                .message("Thông tin giao dịch")
                .result(transactionService.getTransactionByTransactionCode(referenceCode))
                .build();
    }
    //    Thanh toán hóa đơn
    @PostMapping("/payments/bills/check")
    @Operation(summary = "Kiểm tra thông tin hóa đơn", description = "Lấy thông tin chi tiết của một hóa đơn từ nhà cung cấp dựa trên mã khách hàng.")
    public ApiResponse<BillDetailsResponse> checkBill(@RequestBody BillCheckRequest request) {
        BillDetailsResponse billDetail = transactionService.checkBill(request);

        if(billDetail==null) {
            return ApiResponse.<BillDetailsResponse>builder()
                    .code(400)
                    .message("Không tìm thấy hóa đơn")
                    .result(null)
                    .build();
        }
        return ApiResponse.<BillDetailsResponse>builder()
                .code(200)
                .message("Thông tin hóa đơn")
                .result(billDetail)
                .build();
    }
    @PostMapping("/payments/bills/pay")
    @Operation(summary = "Thực hiện thanh toán hóa đơn", description = "Xác nhận và thanh toán cho một hóa đơn đã được kiểm tra.")
    public ApiResponse<TransactionDTO> payBill(@RequestBody BillPaymentRequest request) {
        return ApiResponse.<TransactionDTO>builder()
                .code(200)
                .message("Thanh toán hóa đơn")
                .result(transactionService.payBill(request))
                .build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/getAllTransactions")
    public ApiResponse getAllTransactions(TransactionFilterRequest request){
        return ApiResponse.builder()
                .code(200)
                .message("Danh sách transaction")
                .result(transactionService.filterTransaction(request))
                .build();
    }

    @GetMapping("/account/getAllPaymentAccount")
    public ApiResponse<List<AccountPaymentResponse>> getALlPaymentAccountByCurrentCustomer() {
        return ApiResponse.<List<AccountPaymentResponse>>builder()
                .code(200)
                .message("Danh sách tài khoản của khách hàng")
                .result(transactionService.getAllAccountPaymentForCurrentCustomer())
                .build();
    }
    @GetMapping("/account/get-customer/{accountNumber}")
    public ApiResponse<CustomerDTO> getCustomerByAccountNumber(@PathVariable String accountNumber){
        return ApiResponse.<CustomerDTO>builder()
                .message("Thông tin khách hàng")
                .code(HttpStatus.OK.value())
                .result(transactionService.getCustomerByAccountNumber(accountNumber))
                .build();
    }
    @GetMapping("/customers/detail")
    public ApiResponse<CustomerResponse> getCustomerDetail() {
        return ApiResponse.<CustomerResponse>builder()
                .code(200)
                .message("Thông tin khách hàng")
                .result(transactionService.getCurrentCustomer())
                .build();
    }
    @GetMapping("/getDailyPaymentTransaction")
    public ApiResponse<Void> getDaily(){
        reconciliationService.getDailyPaymentTransaction();
        return ApiResponse.<Void>builder()
                .code(200)
                .message("Tác vụ đối soát")
                .build();
    }


    @GetMapping("/get-latest-transaction/{fromAccountNumber}")
    public ApiResponse<List<InforTransactionLatestResponse>> getListToAccountNumberLatest(@PathVariable String fromAccountNumber ){
        return ApiResponse.<List<InforTransactionLatestResponse>>builder()
                .code(200)
                .message("Danh sách các số tài khoản đã giao dịch mới nhất")
                .result(transactionService.getListToAccountNumberLatest(fromAccountNumber))
                .build();
    }
    @GetMapping("/getTransactionsAndFilter")
    public ApiResponse getTransactionsAndFilter(TransactionFilterRequest request){
        return ApiResponse.builder()
                .code(200)
                .message("Danh sách transaction")
                .result(transactionService.filterTransaction(request))
                .build();
    }
    @GetMapping("/getFilterMetadata")
    public ApiResponse<FilterMetadataResponse> getOptionData(){
        return ApiResponse.<FilterMetadataResponse>builder()
                .code(200)
                .message("Danh sách enums")
                .result(transactionService.getFilterMetadata())
                .build();
    }
    @GetMapping("/paybill/providers")
    public ApiResponse<Map<String, List<ProviderDTO>>> getAvailableProviders() {
        Map<String, List<ProviderDTO>> providers = transactionService.getGroupedProviders();

        if (providers.isEmpty()) {
            return ApiResponse.<Map<String, List<ProviderDTO>>>builder()
                    .code(400)
                    .message("Không có dữ liệu")
                    .result(null)
                    .build();
        }
        return ApiResponse.<Map<String, List<ProviderDTO>>>builder()
                .code(400)
                .message("Danh sách nhà cung cấp")
                .result(providers)
                .build();
    }

    @PostMapping("/inquiry-destination-account")
    public ApiResponse<NapasInquiryResponse> checkDestinationAccount(@RequestBody @Valid NapasInquiryRequest request) {
        return ApiResponse.<NapasInquiryResponse>builder()
                .code(200)
                .message("Kiểm tra thông tin tài khoản đến")
                .result(transactionService.checkDestinationAccount(request))
                .build();
    }
    @PostMapping("/external-transfer")
    public ApiResponse<TransactionDTO> externalTransfer(@RequestBody @Valid ExternalTransferRequest request) {
        return ApiResponse.<TransactionDTO>builder()
                .code(200)
                .message("Chuyển khoản liên ngân hàng")
                .result(transactionService.transferToExternalBank(request))
                .build();
    }

}