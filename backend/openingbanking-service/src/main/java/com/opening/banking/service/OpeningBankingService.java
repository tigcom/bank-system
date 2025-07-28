package com.opening.banking.service;


import com.opening.banking.request.InfoIncomeRequestDto;
import com.opening.banking.response.TransactionDto;

import java.util.List;

public interface OpeningBankingService {
    List<TransactionDto> checkIncome(InfoIncomeRequestDto infoIncome);
}
