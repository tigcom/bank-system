package com.example.transaction_service.dubbo;

import com.example.common_service.dto.CommonTransactionDTO;
import com.example.common_service.dto.request.*;
import com.example.common_service.services.transactions.CommonTransactionService;
import com.example.transaction_service.dto.TransactionDTO;
import com.example.transaction_service.dto.request.*;
import com.example.transaction_service.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService
@RequiredArgsConstructor
public class TransactionServiceDubboImpl implements CommonTransactionService {

    private final TransactionService transactionService;

    @Override
    public CommonTransactionDTO loanPayment(PayRepaymentRequest paymentRequest) {
        PaymentRequest request = PaymentRequest.builder()
                .fromAccountNumber(paymentRequest.getFromAccountNumber())
                .amount(paymentRequest.getAmount())
                .currency(paymentRequest.getCurrency())
                .description(paymentRequest.getDescription())
                .build();
        TransactionDTO transactionDTO = transactionService.payBill(request);
        return toCommonTransactionDTO(transactionDTO);
    }

    @Override
    public CommonTransactionDTO loanDisbursement(CommonDisburseRequest disburseRequest) {
        DisburseRequest request = DisburseRequest.builder()
                .toAccountNumber(disburseRequest.getToAccountNumber())
                .amount(disburseRequest.getAmount())
                .currency(disburseRequest.getCurrency())
                .description(disburseRequest.getDescription())
                .build();
        TransactionDTO transactionDTO = transactionService.disburse(request);
        return toCommonTransactionDTO(transactionDTO);
    }

    @Override
    public CommonTransactionDTO deposit(CommonDepositRequest depositRequest) {
        DepositRequest request = DepositRequest.builder()
                .toAccountNumber(depositRequest.getToAccountNumber())
                .amount(depositRequest.getAmount())
                .currency(depositRequest.getCurrency())
                .description(depositRequest.getDescription())
                .build();
        TransactionDTO transactionDTO = transactionService.deposit(request);
        return toCommonTransactionDTO(transactionDTO);
    }

    @Override
    public CommonTransactionDTO createAccountSaving(CreateAccountSavingRequest accountSavingRequest) {
        TransactionDTO transactionDTO = transactionService.createAccountSaving(accountSavingRequest);
        return toCommonTransactionDTO(transactionDTO);
    }

    @Override
    public CommonTransactionDTO confirmTransaction(CommonConfirmTransactionRequest commonConfirmTransaction) {
        ConfirmTransactionRequest request = ConfirmTransactionRequest.builder()
                .referenceCode(commonConfirmTransaction.getReferenceCode())
                .otpCode(commonConfirmTransaction.getOtpCode())
                .build();
        TransactionDTO transactionDTO = transactionService.confirmTransaction(request);
        return toCommonTransactionDTO(transactionDTO);
    }

    @Override
    public void reSendOtp(CommonResendOtpRequest resendOtpRequest) {
        ResendOtpRequest request = ResendOtpRequest.builder()
                .accountNumberRecipient(resendOtpRequest.getAccountNumberRecipient())
                .referenceCode(resendOtpRequest.getReferenceCode())
                .build();
        transactionService.resendOtp(request);
    }

    private CommonTransactionDTO toCommonTransactionDTO(TransactionDTO transactionDTO){
        CommonTransactionDTO commonTransactionDTO = CommonTransactionDTO.builder()
                .amount(transactionDTO.getAmount())
                .type(transactionDTO.getType())
                .timestamp(transactionDTO.getTimestamp())
                .status(transactionDTO.getStatus())
                .fromAccountNumber(transactionDTO.getFromAccountNumber())
                .toAccountNumber(transactionDTO.getToAccountNumber())
                .currency(transactionDTO.getCurrency())
                .referenceCode(transactionDTO.getReferenceCode())
                .failedReason(transactionDTO.getFailedReason())
                .build();
        return commonTransactionDTO;
    }
}
