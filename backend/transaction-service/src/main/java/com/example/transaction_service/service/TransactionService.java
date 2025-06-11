package com.example.transaction_service.service;

import com.example.common_service.dto.request.CreateAccountSavingRequest;
import com.example.common_service.dto.request.WithdrawAccountSavingRequest;
import com.example.transaction_service.dto.TransactionDTO;
import com.example.transaction_service.dto.request.*;
import com.example.transaction_service.dto.response.FilterMetadataResponse;
import com.example.transaction_service.dto.response.InforTransactionLatestResponse;
import com.example.transaction_service.entity.Transaction;
import org.springframework.data.domain.Page;

import java.util.List;

public interface TransactionService {
    TransactionDTO transfer(TransferRequest transferRequest);
    TransactionDTO deposit(DepositRequest depositRequest);
    TransactionDTO withdraw(WithdrawRequest withdrawRequest);
    TransactionDTO payBill(PaymentRequest repaymentRequest);
    TransactionDTO disburse(DisburseRequest disburseRequest);
    TransactionDTO createAccountSaving(CreateAccountSavingRequest accountSavingRequest);
    TransactionDTO confirmTransaction(ConfirmTransactionRequest confirmTransactionRequest);

    TransactionDTO withdrawAccountSaving(WithdrawAccountSavingRequest depositAccountSavingRequest);

    TransactionDTO transferToExternalBank(ExternalTransferRequest externalTransferRequest);
    void resendOtp(ResendOtpRequest resendOtpRequest);
    TransactionDTO getTransactionById(String transactionId);
    List<TransactionDTO> getAccountTransactions(String accountNumber);
    TransactionDTO getTransactionByTransactionCode(String referenceCode);

    List<InforTransactionLatestResponse> getListToAccountNumberLatest(String fromAccountNumber);

    Page<Transaction> filterTransaction(TransactionFilterRequest request);

    FilterMetadataResponse getFilterMetadata();
}
