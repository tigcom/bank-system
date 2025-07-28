package com.example.loan_service.service;



import com.example.loan_service.dto.request.InfoIncomeRequestDto;
import com.example.loan_service.dto.response.TransactionDto;

import java.util.List;

public interface OpeningBankingClient {
    List<TransactionDto> checkIncome(InfoIncomeRequestDto infoIncome);
}

