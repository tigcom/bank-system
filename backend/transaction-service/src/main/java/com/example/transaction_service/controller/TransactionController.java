//package com.example.transaction_service.controller;
//
//
//import com.example.transaction_service.dto.TransactionDTO;
//import com.example.transaction_service.dto.request.*;
//import com.example.transaction_service.dto.response.ApiResponse;
////import com.example.transaction_service.service.TransactionService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.media.Content;
//import io.swagger.v3.oas.annotations.media.ExampleObject;
//import io.swagger.v3.oas.annotations.media.Schema;
//import io.swagger.v3.oas.annotations.responses.ApiResponses;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping(value = "/api/transactions")
//@RequiredArgsConstructor
//@Tag(name = "Giao dịch", description = "Các API xử lý giao dịch tài khoản như chuyển tiền, nạp tiền, rút tiền, thanh toán hóa đơn...")
//public class TransactionController {
//    private final TransactionService transactionService;
//
//    @Operation(summary = "Chuyển khoản", description = "Chuyển tiền từ tài khoản nguồn đến tài khoản đích.")
//    @ApiResponses(value = {
//            @io.swagger.v3.oas.annotations.responses.ApiResponse(
//                    responseCode = "200",
//                    description = "Chuyển khoản thành công",
//                    content = @Content(
//                            mediaType = "application/json",
//                            schema = @Schema(implementation = ApiResponse.class),
//                            examples = @ExampleObject(value = """
//                    {
//                        "code": 200,
//                        "message": "Chuyển khoản",
//                        "result": {
//                                "id": "abc123",
//                                 "fromAccountNumber": null,
//                                 "toAccountNumber": "970452999999999",
//                                 "amount": 500000,
//                                 "description": "Nạp tiền mặt",
//                                  "timestamp": "2025-05-29T10:00:00",
//                                  "status": "SUCCESS",
//                                  "type": "TRANSFER",
//                                  "currency": "VND",
//                                  "referenceCode": "TXN-970452999999999-20250529100000abc",
//                                  "failedReason": null
//                                 }
//                               }
//                          """)
//                    )
//            ),
//            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
//    })
//    @PostMapping("/transfer")
//    public ApiResponse<TransactionDTO> transfer(@RequestBody @Valid TransferRequest request) {
//        return ApiResponse.<TransactionDTO>builder()
//                .code(200)
//                .message("Chuyển khoản")
//                .result(transactionService.transfer(request))
//                .build();
//    }
//
//
//    @Operation(summary = "Nạp tiền", description = "Nạp tiền vào tài khoản.")
//    @ApiResponses(value = {
//            @io.swagger.v3.oas.annotations.responses.ApiResponse(
//                    responseCode = "200",
//                    description = "Nạp tiền thành công",
//                    content = @Content(
//                            mediaType = "application/json",
//                            schema = @Schema(implementation = ApiResponse.class),
//                            examples = @ExampleObject(value = """
//                    {
//                        "code": 200,
//                        "message": "Nạp tiền vào tài khoản",
//                        "result": {
//                                "id": "abc123",
//                                 "fromAccountNumber": null,
//                                 "toAccountNumber": "970452999999999",
//                                 "amount": 500000,
//                                 "description": "Nạp tiền mặt",
//                                  "timestamp": "2025-05-29T10:00:00",
//                                  "status": "SUCCESS",
//                                  "type": "DEPOSIT",
//                                  "currency": "VND",
//                                  "referenceCode": "TXN-970452999999999-20250529100000abc",
//                                  "failedReason": null
//                                 }
//                               }
//                          """)
//                    )
//            ),
//            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
//    })
//    @PostMapping("/deposit")
//    public ApiResponse<TransactionDTO> deposit(@RequestBody @Valid DepositRequest request) {
//        return ApiResponse.<TransactionDTO>builder()
//                .code(200)
//                .message("Nạp tiền vào tài khoản")
//                .result(transactionService.deposit(request))
//                .build();
//    }
//
//    @Operation(summary = "Rút tiền", description = "Rút tiền từ tài khoản.")
//    @ApiResponses(value = {
//            @io.swagger.v3.oas.annotations.responses.ApiResponse(
//                    responseCode = "200",
//                    description = "Thông tin chi tiết giao dịch",
//                    content = @Content(
//                            mediaType = "application/json",
//                            schema = @Schema(implementation = ApiResponse.class),
//                            examples = @ExampleObject(value = """
//            {
//                "code": 200,
//                "message": "Thông tin chi tiết giao dịch",
//                "result": {
//                    "id": "e1d62ed3-5a39-4100-89e8-46f6a85a0a80",
//                    "fromAccountNumber": "970452999999999",
//                    "toAccountNumber": "87654321",
//                    "amount": 10000.00,
//                    "description": "Rút tiền ",
//                    "timestamp": "2025-05-29T10:44:43.419631",
//                    "status": "COMPLETED",
//                    "type": "WITHDRAW",
//                    "currency": "VND",
//                    "referenceCode": "TXN-970452999999999-20250529104443f6e662cf",
//                    "failedReason": ""
//                }
//            }
//            """)
//                    )
//            ),
//            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc OTP sai")
//    })
//    @PostMapping("/withdraw")
//    public ApiResponse<TransactionDTO> withdraw(@RequestBody @Valid WithdrawRequest request) {
//        return ApiResponse.<TransactionDTO>builder()
//                .code(200)
//                .message("Rút tiền")
//                .result(transactionService.withdraw(request))
//                .build();
//    }
//
//    @Operation(summary = "Thanh toán hóa đơn", description = "Thanh toán hóa đơn cho tài khoản.")
//    @ApiResponses(value = {
//            @io.swagger.v3.oas.annotations.responses.ApiResponse(
//                    responseCode = "200",
//                    description = "Thông tin chi tiết giao dịch",
//                    content = @Content(
//                            mediaType = "application/json",
//                            schema = @Schema(implementation = ApiResponse.class),
//                            examples = @ExampleObject(value = """
//            {
//                "code": 200,
//                "message": "Thông tin chi tiết giao dịch",
//                "result": {
//                    "id": "e1d62ed3-5a39-4100-89e8-46f6a85a0a80",
//                    "fromAccountNumber": "970452999999999",
//                    "toAccountNumber": "87654321",
//                    "amount": 10000.00,
//                    "description": "Thanh toán hóa đơn ",
//                    "timestamp": "2025-05-29T10:44:43.419631",
//                    "status": "COMPLETED",
//                    "type": "PAY_BILL",
//                    "currency": "VND",
//                    "referenceCode": "TXN-970452999999999-20250529104443f6e662cf",
//                    "failedReason": ""
//                }
//            }
//            """)
//                    )
//            ),
//            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc OTP sai")
//    })
//    @PostMapping("/pay-bill")
//    public ApiResponse<TransactionDTO> payBill(@RequestBody @Valid PaymentRequest request) {
//        return ApiResponse.<TransactionDTO>builder()
//                .code(200)
//                .message("Thanh toán hóa đơn")
//                .result(transactionService.payBill(request))
//                .build();
//    }
//
//    @Operation(summary = "Giải ngân", description = "Giải ngân khoản vay cho khách hàng.")
//    @ApiResponses(value = {
//            @io.swagger.v3.oas.annotations.responses.ApiResponse(
//                    responseCode = "200",
//                    description = "Thông tin chi tiết giao dịch",
//                    content = @Content(
//                            mediaType = "application/json",
//                            schema = @Schema(implementation = ApiResponse.class),
//                            examples = @ExampleObject(value = """
//            {
//                "code": 200,
//                "message": "Thông tin chi tiết giao dịch",
//                "result": {
//                    "id": "e1d62ed3-5a39-4100-89e8-46f6a85a0a80",
//                    "fromAccountNumber": "970452999999999",
//                    "toAccountNumber": "87654321",
//                    "amount": 10000.00,
//                    "description": "Thanh toán hóa đơn ",
//                    "timestamp": "2025-05-29T10:44:43.419631",
//                    "status": "COMPLETED",
//                    "type": "DISBURSEMENT",
//                    "currency": "VND",
//                    "referenceCode": "TXN-970452999999999-20250529104443f6e662cf",
//                    "failedReason": ""
//                }
//            }
//            """)
//                    )
//            ),
//            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc OTP sai")
//    })
//    @PostMapping("/disburse")
//    public ApiResponse<TransactionDTO> disburse(@RequestBody @Valid DisburseRequest request) {
//        return ApiResponse.<TransactionDTO>builder()
//                .code(200)
//                .message("Giải ngân khoản vay")
//                .result(transactionService.disburse(request ))
//                .build();
//    }
//    @Operation(summary = "Xác nhận giao dịch", description = "Xác nhận OTP để hoàn tất giao dịch.")
//    @ApiResponses(value = {
//            @io.swagger.v3.oas.annotations.responses.ApiResponse(
//                    responseCode = "200",
//                    description = "Thông tin chi tiết giao dịch",
//                    content = @Content(
//                            mediaType = "application/json",
//                            schema = @Schema(implementation = ApiResponse.class),
//                            examples = @ExampleObject(value = """
//            {
//                "code": 200,
//                "message": "Thông tin chi tiết giao dịch",
//                "result": {
//                    "id": "e1d62ed3-5a39-4100-89e8-46f6a85a0a80",
//                    "fromAccountNumber": "970452999999999",
//                    "toAccountNumber": "87654321",
//                    "amount": 10000.00,
//                    "description": "Giải ngân tiền ",
//                    "timestamp": "2025-05-29T10:44:43.419631",
//                    "status": "COMPLETED",
//                    "type": "DISBURSEMENT",
//                    "currency": "VND",
//                    "referenceCode": "TXN-970452999999999-20250529104443f6e662cf",
//                    "failedReason": ""
//                }
//            }
//            """)
//                    )
//            ),
//            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc OTP sai")
//    })
//    @PostMapping("/confirm-transaction")
//    public ApiResponse<TransactionDTO> confirmTransaction(@RequestBody @Valid ConfirmTransactionRequest request) {
//        return ApiResponse.<TransactionDTO>builder()
//                .code(200)
//                .message("Thông tin chi tiết giao dịch")
//                .result(transactionService.confirmTransaction(request))
//                .build();
//    }
//    @Operation(summary = "Gửi lại OTP", description = "Gửi lại mã OTP cho mã giao dịch.")
//    @ApiResponses(value = {
//            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OTP đã được gửi lại thành công"),
//            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Giao dịch không hợp lệ")
//    })
//    @PostMapping("/resend-otp")
//    public ApiResponse<String> resendOtp(@RequestBody ResendOtpRequest resendOtpRequest) {
//        transactionService.resendOtp(resendOtpRequest);
//        return ApiResponse.<String>builder()
//                .code(200)
//                .message("Mã OTP mới đã được gửi thành công")
//                .build();
//    }
//
//    @Operation(summary = "Lịch sử giao dịch", description = "Lấy danh sách giao dịch của một tài khoản.")
//    @ApiResponses(value = {
//            @io.swagger.v3.oas.annotations.responses.ApiResponse(
//                    responseCode = "200",
//                    description = "Thông tin chi tiết giao dịch",
//                    content = @Content(
//                            mediaType = "application/json",
//                            schema = @Schema(implementation = ApiResponse.class),
//                            examples = @ExampleObject(value = """
//            {
//                "code": 200,
//                "message": "Thông tin chi tiết giao dịch",
//                "result": {
//                    "id": "e1d62ed3-5a39-4100-89e8-46f6a85a0a80",
//                    "fromAccountNumber": "970452999999999",
//                    "toAccountNumber": "87654321",
//                    "amount": 10000.00,
//                    "description": "Giải ngân tiền ",
//                    "timestamp": "2025-05-29T10:44:43.419631",
//                    "status": "COMPLETED",
//                    "type": "DISBURSEMENT",
//                    "currency": "VND",
//                    "referenceCode": "TXN-970452999999999-20250529104443f6e662cf",
//                    "failedReason": ""
//                }
//            }
//            """)
//                    )
//            ),
//            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc OTP sai")
//    })
//    @GetMapping("/account/{accountNumber}")
//    public ApiResponse<List<TransactionDTO>> getTransactionsByAccount(@PathVariable String accountNumber){
//        return ApiResponse.<List<TransactionDTO>>builder()
//                .code(200)
//                .message("Danh sách giao dịch của tài khoản")
//                .result(transactionService.getAccountTransactions(accountNumber))
//                .build();
//    }
//
//    @Operation(summary = "Chi tiết giao dịch", description = "Lấy chi tiết giao dịch theo mã tham chiếu.")
//    @ApiResponses(value = {
//            @io.swagger.v3.oas.annotations.responses.ApiResponse(
//                    responseCode = "200",
//                    description = "Thông tin chi tiết giao dịch",
//                    content = @Content(
//                            mediaType = "application/json",
//                            schema = @Schema(implementation = ApiResponse.class),
//                            examples = @ExampleObject(value = """
//            {
//                "code": 200,
//                "message": "Thông tin chi tiết giao dịch",
//                "result": {
//                    "id": "e1d62ed3-5a39-4100-89e8-46f6a85a0a80",
//                    "fromAccountNumber": "970452999999999",
//                    "toAccountNumber": "87654321",
//                    "amount": 10000.00,
//                    "description": "Giải ngân tiền ",
//                    "timestamp": "2025-05-29T10:44:43.419631",
//                    "status": "COMPLETED",
//                    "type": "DISBURSEMENT",
//                    "currency": "VND",
//                    "referenceCode": "TXN-970452999999999-20250529104443f6e662cf",
//                    "failedReason": ""
//                }
//            }
//            """)
//                    )
//            ),
//            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc OTP sai")
//    })
//    @GetMapping("/{referenceCode}")
//    public ApiResponse<TransactionDTO> getTransactionByReferenceCode(@PathVariable String referenceCode){
//        return ApiResponse.<TransactionDTO>builder()
//                .code(200)
//                .message("Thông tin giao dịch")
//                .result(transactionService.getTransactionByTransactionCode(referenceCode))
//                .build();
//    }
//}
