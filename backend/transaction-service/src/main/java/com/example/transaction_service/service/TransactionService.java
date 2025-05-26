package com.example.transaction_service.service;

import com.example.transaction_service.dto.TransactionDTO;
import com.example.transaction_service.dto.request.DepositRequest;
import com.example.transaction_service.dto.request.TransferRequest;
import com.example.transaction_service.dto.request.WithdrawRequest;
import com.example.transaction_service.enums.TransactionStatus;

import java.util.List;

public interface TransactionService {
    TransactionDTO transfer(TransferRequest transferRequest);
    TransactionDTO deposit(DepositRequest depositRequest);
    TransactionDTO withdraw(WithdrawRequest withdrawRequest);
    TransactionDTO payBill(TransactionDTO transactionDTO);
    TransactionDTO getTransactionById(String transactionId);
    List<TransactionDTO> getTransactionByAccount(String accountId);
    TransactionDTO getTransactionByTransactionCode(String referenceCode);
    TransactionDTO updateTransactionsStatus(String transactionId, TransactionStatus status);
}
