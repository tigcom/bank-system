package com.example.corebanking_service.controller;


import com.example.common_service.dto.CommonTransactionDTO;
import com.example.corebanking_service.dto.request.TransactionRequest;
import com.example.corebanking_service.dto.response.ApiResponse;
import com.example.corebanking_service.service.CoreTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping(value = "/api/core-bank")
@RequiredArgsConstructor
public class CoreTransactionController {
    private final CoreTransactionService coreTransactionService;

    @GetMapping("/get-balance/{accountNumber}")
    public ApiResponse<BigDecimal> getBalance(@PathVariable String accountNumber){
        return ApiResponse.<BigDecimal>builder()
                .code(200)
                .message("Số dư trong tài khoản")
                .result(coreTransactionService.getBalance(accountNumber))
                .build();
    }
    @PostMapping("/perform-transaction")
    public ApiResponse<CommonTransactionDTO> performTransfer(@RequestBody TransactionRequest request){
        return ApiResponse.<CommonTransactionDTO>builder()
                .code(200)
                .message("Thực hiện chuyển tiền")
                .result(coreTransactionService.performTransfer(request))
                .build();

    }
    @PostMapping("/reverse-transaction")
    public ApiResponse<Void> reverseTransaction(@RequestBody TransactionRequest request) {
        coreTransactionService.reverseTransaction(request);
        return ApiResponse.<Void>builder()
                .code(200)
                .message("Giao dịch hoàn tiền")
                .build();
    }
}