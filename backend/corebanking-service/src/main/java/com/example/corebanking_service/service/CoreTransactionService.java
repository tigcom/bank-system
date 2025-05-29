package com.example.corebanking_service.service;

import com.example.common_service.dto.CommonTransactionDTO;
import com.example.corebanking_service.dto.request.TransactionRequest;

import java.math.BigDecimal;

public interface CoreTransactionService {
    CommonTransactionDTO performTransfer(TransactionRequest request);
    BigDecimal getBalance(String accountNumber);
}
