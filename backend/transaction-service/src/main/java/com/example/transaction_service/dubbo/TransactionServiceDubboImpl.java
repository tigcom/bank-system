package com.example.transaction_service.dubbo;

import com.example.common_service.dto.CommonTransactionDTO;
import com.example.common_service.dto.request.*;
import com.example.common_service.services.transactions.CommonTransactionService;
import com.example.transaction_service.dto.TransactionDTO;
import com.example.transaction_service.dto.request.*;
import com.example.transaction_service.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DubboService
@RequiredArgsConstructor
public class TransactionServiceDubboImpl implements CommonTransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionServiceDubboImpl.class);
    private final TransactionService transactionService;

    @Override
    public CommonTransactionDTO loanPayment(PayRepaymentRequest paymentRequest) {
        log.info("[DUBBO][loanPayment] Nhận yêu cầu thanh toán khoản vay: {}", paymentRequest);
        try {
            LoanPaymentRequest request = LoanPaymentRequest.builder()
                    .fromAccountNumber(paymentRequest.getFromAccountNumber())
                    .amount(paymentRequest.getAmount())
                    .currency(paymentRequest.getCurrency())
                    .description(paymentRequest.getDescription())
                    .build();
            TransactionDTO transactionDTO = transactionService.payBillLoan(request);
            log.info("[DUBBO][loanPayment] Kết quả: {}", transactionDTO);
            return toCommonTransactionDTO(transactionDTO);
        } catch (Exception ex) {
            log.error("[DUBBO][loanPayment] Lỗi: {}", ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    public CommonTransactionDTO loanDisbursement(CommonDisburseRequest disburseRequest) {
        log.info("[DUBBO][loanDisbursement] Nhận yêu cầu giải ngân: {}", disburseRequest);
        try {
            DisburseRequest request = DisburseRequest.builder()
                    .toAccountNumber(disburseRequest.getToAccountNumber())
                    .amount(disburseRequest.getAmount())
                    .currency(disburseRequest.getCurrency())
                    .description(disburseRequest.getDescription())
                    .build();
            TransactionDTO transactionDTO = transactionService.disburse(request);
            log.info("[DUBBO][loanDisbursement] Kết quả: {}", transactionDTO);
            return toCommonTransactionDTO(transactionDTO);
        } catch (Exception ex) {
            log.error("[DUBBO][loanDisbursement] Lỗi: {}", ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    public CommonTransactionDTO deposit(CommonDepositRequest depositRequest) {
        log.info("[DUBBO][deposit] Nhận yêu cầu nạp tiền: {}", depositRequest);
        try {
            DepositRequest request = DepositRequest.builder()
                    .toAccountNumber(depositRequest.getToAccountNumber())
                    .amount(depositRequest.getAmount())
                    .currency(depositRequest.getCurrency())
                    .description(depositRequest.getDescription())
                    .build();
            TransactionDTO transactionDTO = transactionService.deposit(request);
            log.info("[DUBBO][deposit] Kết quả: {}", transactionDTO);
            return toCommonTransactionDTO(transactionDTO);
        } catch (Exception ex) {
            log.error("[DUBBO][deposit] Lỗi: {}", ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    public CommonTransactionDTO createAccountSaving(CreateAccountSavingRequest accountSavingRequest) {
        log.info("[DUBBO][createAccountSaving] Nhận yêu cầu mở sổ tiết kiệm: {}", accountSavingRequest);
        try {
            TransactionDTO transactionDTO = transactionService.createAccountSaving(accountSavingRequest);
            log.info("[DUBBO][createAccountSaving] Kết quả: {}", transactionDTO);
            return toCommonTransactionDTO(transactionDTO);
        } catch (Exception ex) {
            log.error("[DUBBO][createAccountSaving] Lỗi: {}", ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    public CommonTransactionDTO confirmTransaction(CommonConfirmTransactionRequest commonConfirmTransaction) {
        log.info("[DUBBO][confirmTransaction] Nhận yêu cầu xác nhận giao dịch: {}", commonConfirmTransaction);
        try {
            ConfirmTransactionRequest request = ConfirmTransactionRequest.builder()
                    .referenceCode(commonConfirmTransaction.getReferenceCode())
                    .otpCode(commonConfirmTransaction.getOtpCode())
                    .build();
            TransactionDTO transactionDTO = transactionService.confirmTransaction(request);
            log.info("[DUBBO][confirmTransaction] Kết quả: {}", transactionDTO);
            return toCommonTransactionDTO(transactionDTO);
        } catch (Exception ex) {
            log.error("[DUBBO][confirmTransaction] Lỗi: {}", ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    public void reSendOtp(CommonResendOtpRequest resendOtpRequest) {
        log.info("[DUBBO][reSendOtp] Nhận yêu cầu gửi lại OTP: {}", resendOtpRequest);
        try {
            ResendOtpRequest request = ResendOtpRequest.builder()
                    .accountNumberRecipient(resendOtpRequest.getAccountNumberRecipient())
                    .referenceCode(resendOtpRequest.getReferenceCode())
                    .build();
            transactionService.resendOtp(request);
            log.info("[DUBBO][reSendOtp] Đã gửi lại OTP thành công");
        } catch (Exception ex) {
            log.error("[DUBBO][reSendOtp] Lỗi: {}", ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    public CommonTransactionDTO withdrawAccountSaving(WithdrawAccountSavingRequest request) {
        log.info("[DUBBO][withdrawAccountSaving] Nhận yêu cầu rút tiết kiệm: {}", request);
        try {
            TransactionDTO transactionDTO = transactionService.withdrawAccountSaving(request);
            log.info("[DUBBO][withdrawAccountSaving] Kết quả: {}", transactionDTO);
            return toCommonTransactionDTO(transactionDTO);
        } catch (Exception ex) {
            log.error("[DUBBO][withdrawAccountSaving] Lỗi: {}", ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    public CommonTransactionDTO payinterestInternal(PayInterestRequest request) {
        TransactionDTO transactionDTO = transactionService.payInterest(request);
        return toCommonTransactionDTO(transactionDTO);
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
