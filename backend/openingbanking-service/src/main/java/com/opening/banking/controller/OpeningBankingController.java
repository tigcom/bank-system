package com.opening.banking.controller;


import com.example.common_service.dto.request.CICRequest;
import com.opening.banking.request.InfoIncomeRequestDto;
import com.opening.banking.response.ApiResponseWrapper;
import com.opening.banking.response.TransactionDto;
import com.opening.banking.service.OpeningBankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/openingbanking-servive")
@RequiredArgsConstructor
public class OpeningBankingController {
    private final OpeningBankingService openingBankingService;
    @PostMapping("/check-income")
    public ApiResponseWrapper<List<TransactionDto>> checkCIC(@RequestBody InfoIncomeRequestDto infoIncome) {
        return ApiResponseWrapper.<List<TransactionDto>>builder()
                .status(HttpStatus.OK.value())
                .message("Check CIC successfully ")
                .data(openingBankingService.checkIncome(infoIncome))
                .build();
    }
}
