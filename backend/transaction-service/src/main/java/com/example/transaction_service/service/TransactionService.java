package com.example.transaction_service.service;

import com.example.transaction_service.dto.TransactionDTO;
import com.example.transaction_service.enums.TransactionStatus;

import java.util.List;

public interface TransactionService {
    TransactionDTO createTransaction(TransactionDTO transactionDTO);
    TransactionDTO getTransactionById(String transactionId);
    List<TransactionDTO> getTransactionByAccount(String accountId);
    TransactionDTO getTransactionByTransactionCode(String referenceCode);
    TransactionDTO updateTransactionsStatus(String transactionId, TransactionStatus status);
}
