package com.example.corebanking_service.service;

import com.example.corebanking_service.dto.request.TransactionRequest;
import com.example.corebanking_service.dto.TransactionDTO;

import java.math.BigDecimal;

public interface CoreTransactionService {
    TransactionDTO performTransfer(TransactionRequest request);
    BigDecimal getBalance(String accountNumber);
}
