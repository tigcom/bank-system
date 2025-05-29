package com.example.transaction_service.service;

import com.example.transaction_service.dto.TransactionDTO;
import com.example.transaction_service.dto.request.*;
import com.example.transaction_service.enums.TransactionStatus;

import java.util.List;

public interface TransactionService {
    TransactionDTO transfer(TransferRequest transferRequest);
    TransactionDTO deposit(DepositRequest depositRequest);
    TransactionDTO withdraw(WithdrawRequest withdrawRequest);
    TransactionDTO payBill(PaymentRequest repaymentRequest);
    TransactionDTO disburse(DisburseRequest disburseRequest);
    TransactionDTO confirmTransaction(ConfirmTransactionRequest confirmTransactionRequest);

    void resendOtp(String referenceCode);
    TransactionDTO getTransactionById(String transactionId);
    List<TransactionDTO> getAccountTransactions(String accountNumber);
    TransactionDTO getTransactionByTransactionCode(String referenceCode);
    TransactionDTO updateTransactionsStatus(String transactionId, TransactionStatus status);
}
