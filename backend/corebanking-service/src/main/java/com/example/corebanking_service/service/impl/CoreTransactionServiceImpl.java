package com.example.corebanking_service.service.impl;

import com.example.corebanking_service.dto.request.TransactionRequest;
import com.example.corebanking_service.dto.TransactionDTO;
import com.example.corebanking_service.entity.CoreAccount;
import com.example.corebanking_service.entity.CoreTransaction;
import com.example.corebanking_service.exception.AppException;
import com.example.corebanking_service.exception.ErrorCode;
import com.example.corebanking_service.repository.CoreAccountRepo;
import com.example.corebanking_service.repository.CoreTransactionRepo;
import com.example.corebanking_service.service.CoreTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@DubboService
@RequiredArgsConstructor
@Slf4j
public class CoreTransactionServiceImpl implements CoreTransactionService {

    private final CoreAccountRepo accountRepo;
    private final CoreTransactionRepo transactionRepo;

    @Override
    @Transactional
    public TransactionDTO performTransfer(TransactionRequest request) {
        CoreAccount fromAccount = accountRepo.findByAccountNumber(request.getFromAccountNumber());
        CoreAccount toAccount = accountRepo.findByAccountNumber(request.getToAccountNumber());
        if (fromAccount == null || toAccount == null) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_EXIST);
        }
        if(fromAccount.getBalance().compareTo(request.getAmount())<0){
            log.warn("Tài khoản {} không đủ tiền. Số dư: {}, Số tiền yêu cầu: {}",
                    fromAccount.getAccountNumber(),
                    fromAccount.getBalance(),
                    request.getAmount());
            throw new AppException(ErrorCode.INSUFFICIENT_FUNDS);
        }
        debit(fromAccount,request.getAmount());
        credit(toAccount,request.getAmount());
        CoreTransaction transaction = CoreTransaction.builder()
                .amount(request.getAmount())
                .transactionType(request.getType())
                .timestamp(request.getTimestamp())
                .status("COMPLETED")
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .build();
        transactionRepo.save(transaction);
        TransactionDTO transactionDTO = TransactionDTO.builder()
                .amount(transaction.getAmount())
                .type(transaction.getTransactionType())
                .timestamp(transaction.getTimestamp())
                .status(transaction.getStatus())
                .fromAccountNumber(transaction.getFromAccount().getAccountNumber())
                .toAccountNumber(transaction.getToAccount().getAccountNumber())
                .build();
        return transactionDTO;
    }

    @Override
    public BigDecimal getBalance(String accountNumber) {
        CoreAccount account = accountRepo.findByAccountNumber(accountNumber);
        if (account == null) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_EXIST);
        }
        return account.getBalance();
    }

    //    Trừ tiền tài khoản nguồn
    private void debit(CoreAccount account, BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0) {
            throw new AppException(ErrorCode.INSUFFICIENT_FUNDS);
        }
        account.setBalance(account.getBalance().subtract(amount));
        accountRepo.save(account);
    }

    //  Cộng tiền ở tài khoản đích
    private void credit(CoreAccount account, BigDecimal amount) {
        account.setBalance(account.getBalance().add(amount));
        accountRepo.save(account);
    }
}
