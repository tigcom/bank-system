package com.example.transaction_service.service;

import com.example.common_service.dto.CustomerDTO;
import com.example.common_service.dto.request.CreateAccountSavingRequest;
import com.example.common_service.dto.request.PayInterestRequest;
import com.example.common_service.dto.request.WithdrawAccountSavingRequest;
import com.example.common_service.dto.response.AccountPaymentResponse;
import com.example.common_service.dto.response.CustomerResponse;
import com.example.transaction_service.dto.TransactionDTO;
import com.example.transaction_service.dto.request.*;
import com.example.transaction_service.dto.response.*;
import com.example.transaction_service.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface TransactionService {
    TransactionDTO transfer(TransferRequest transferRequest);
    TransactionDTO deposit(DepositRequest depositRequest);
    TransactionDTO withdraw(WithdrawRequest withdrawRequest);
    TransactionDTO payBillLoan(LoanPaymentRequest repaymentRequest);
    BillDetailsResponse checkBill(BillCheckRequest request);

    TransactionDTO payBill(BillPaymentRequest request);
    Map<String, List<ProviderDTO>> getGroupedProviders();
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

    NapasInquiryResponse checkDestinationAccount(NapasInquiryRequest request);

    Page<TransactionDTO> getAccountTransactions(String accountNumber, Pageable pageable);

    TransactionDTO payInterest(PayInterestRequest request);

    List<AccountPaymentResponse> getAllAccountPaymentForCurrentCustomer();
    CustomerDTO getCustomerByAccountNumber(String accountNumber);
    CustomerResponse getCurrentCustomer();

    TransactionStatsResponse getTransactionStats(LocalDateTime startDate, LocalDateTime endDate,Pageable pageable);
}