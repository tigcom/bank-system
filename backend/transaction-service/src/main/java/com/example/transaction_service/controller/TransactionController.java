package com.example.transaction_service.controller;


import com.example.transaction_service.dto.TransactionDTO;
import com.example.transaction_service.dto.request.DepositRequest;
import com.example.transaction_service.dto.request.TransferRequest;
import com.example.transaction_service.dto.request.WithdrawRequest;
import com.example.transaction_service.dto.response.ApiResponse;
import com.example.transaction_service.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;

    @PostMapping("/transfer")
    public ApiResponse<TransactionDTO> transfer(@RequestBody @Valid TransferRequest request) {
        return ApiResponse.<TransactionDTO>builder()
                .code(200)
                .message("Chuyển thành công")
                .result(transactionService.transfer(request))
                .build();
    }

    @PostMapping("/deposit")
    public ApiResponse<TransactionDTO> deposit(@RequestBody @Valid DepositRequest request) {
        return ApiResponse.<TransactionDTO>builder()
                .code(200)
                .message("Nộp tiền thành công")
                .result(transactionService.deposit(request))
                .build();
    }

    @PostMapping("/withdraw")
    public ApiResponse<TransactionDTO> withdraw(@RequestBody @Valid WithdrawRequest request) {
        return ApiResponse.<TransactionDTO>builder()
                .code(200)
                .message("Rút tiền thành công")
                .result(transactionService.withdraw(request))
                .build();
    }
}
