package com.example.transaction_service.service.impl;

import com.example.transaction_service.dto.TransactionDTO;
import com.example.transaction_service.enums.TransactionStatus;
import com.example.transaction_service.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {



    @Override
    public TransactionDTO createTransaction(TransactionDTO transactionDTO) {
        return null;
    }

    @Override
    public TransactionDTO getTransactionById(String transactionId) {
        return null;
    }

    @Override
    public List<TransactionDTO> getTransactionByAccount(String accountId) {
        return null;
    }

    @Override
    public TransactionDTO getTransactionByTransactionCode(String referenceCode) {
        return null;
    }

    @Override
    public TransactionDTO updateTransactionsStatus(String transactionId, TransactionStatus status) {
        return null;
    }
}
