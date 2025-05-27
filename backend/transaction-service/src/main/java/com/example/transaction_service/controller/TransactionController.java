package com.example.transaction_service.controller;


import com.example.common_service.dto.PayRepaymentRequest;
import com.example.transaction_service.dto.TransactionDTO;
import com.example.transaction_service.dto.request.*;
import com.example.transaction_service.dto.response.ApiResponse;
import com.example.transaction_service.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;

    @PostMapping("/transfer")
    public ApiResponse<TransactionDTO> transfer(@RequestBody @Valid TransferRequest request) {
        return ApiResponse.<TransactionDTO>builder()
                .code(200)
                .message("Chuyển khoản")
                .result(transactionService.transfer(request))
                .build();
    }

    @PostMapping("/deposit")
    public ApiResponse<TransactionDTO> deposit(@RequestBody @Valid DepositRequest request) {
        return ApiResponse.<TransactionDTO>builder()
                .code(200)
                .message("Nạp tiền vào tài khoản")
                .result(transactionService.deposit(request))
                .build();
    }

    @PostMapping("/withdraw")
    public ApiResponse<TransactionDTO> withdraw(@RequestBody @Valid WithdrawRequest request) {
        return ApiResponse.<TransactionDTO>builder()
                .code(200)
                .message("Rút tiền")
                .result(transactionService.withdraw(request))
                .build();
    }
    @PostMapping("/pay-bill")
    public ApiResponse<TransactionDTO> payBill(@RequestBody @Valid PayRepaymentRequest request) {
        return ApiResponse.<TransactionDTO>builder()
                .code(200)
                .message("Thanh toán hóa đơn")
                .result(transactionService.payBill(request))
                .build();
    }
    @PostMapping("/disburse")
    public ApiResponse<TransactionDTO> disburse(@RequestBody @Valid DisburseRequest request) {
        return ApiResponse.<TransactionDTO>builder()
                .code(200)
                .message("Giải ngân khoản vay")
                .result(transactionService.disburse(request ))
                .build();
    }
    @PostMapping("/confirm-transaction")
    public ApiResponse<TransactionDTO> confirmTransaction(@RequestBody @Valid ConfirmTransactionRequest request) {
        return ApiResponse.<TransactionDTO>builder()
                .code(200)
                .message("Giao dịch thành công")
                .result(transactionService.confirmTransaction(request))
                .build();
    }
    @PostMapping("/{referenceCode}/resend-otp")
    public ApiResponse<String> resendOtp(@PathVariable String referenceCode) {
        transactionService.resendOtp(referenceCode);
        return ApiResponse.<String>builder()
                .code(200)
                .message("Mã OTP mới đã được gửi thành công")
                .build();
    }

    @GetMapping("/account/{accountNumber}")
    public ApiResponse<List<TransactionDTO>> getTransactionsByAccount(@PathVariable String accountNumber){
        return ApiResponse.<List<TransactionDTO>>builder()
                .code(200)
                .message("Danh sách giao dịch của tài khoản")
                .result(transactionService.getAccountTransactions(accountNumber))
                .build();
    }
    @GetMapping("/{referenceCode}")
    public ApiResponse<TransactionDTO> getTransactionByReferenceCode(@PathVariable String referenceCode){
        return ApiResponse.<TransactionDTO>builder()
                .code(200)
                .message("Thông tin giao dịch")
                .result(transactionService.getTransactionByTransactionCode(referenceCode))
                .build();
    }
}
