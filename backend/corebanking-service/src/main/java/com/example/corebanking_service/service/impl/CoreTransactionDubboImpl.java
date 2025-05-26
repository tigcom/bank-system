package com.example.corebanking_service.service.impl;

import com.example.common_service.dto.TransactionRequest;
import com.example.common_service.services.transaction.CoreTransactionService;
import com.example.corebanking_service.entity.CoreAccount;
import com.example.corebanking_service.entity.CoreTransaction;
import com.example.corebanking_service.exception.AppException;
import com.example.corebanking_service.exception.ErrorCode;
import com.example.corebanking_service.repository.CoreAccountRepo;
import com.example.corebanking_service.repository.CoreTransactionRepo;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@DubboService
@RequiredArgsConstructor
public class CoreTransactionDubboImpl implements CoreTransactionService {

    private final CoreAccountRepo accountRepo;
    private final CoreTransactionRepo transactionRepo;

    @Override
    @Transactional
    public void performTransfer(TransactionRequest request) {
        CoreAccount fromAccount = accountRepo.findByAccountNumber(request.getFromAccountNumber());
        CoreAccount toAccount = accountRepo.findByAccountNumber(request.getToAccountNumber());

        debit(fromAccount,request.getAmount());
        credit(toAccount,request.getAmount());
        CoreTransaction transaction = CoreTransaction.builder()
                .amount(request.getAmount())
                .transactionType(request.getType())
                .timestamp(request.getTimestamp())
                .status("COMPLETED")
                .fromAccount(accountRepo.findByAccountNumber(request.getFromAccountNumber()))
                .toAccount(accountRepo.findByAccountNumber(request.getToAccountNumber()))
                .build();
        transactionRepo.save(transaction);
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
