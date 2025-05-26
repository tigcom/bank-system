package com.example.corebanking_service.service.impl;

import com.example.common_service.dto.AccountDTO;
import com.example.common_service.services.account.AccountQueryService;
import com.example.corebanking_service.entity.CoreAccount;
import com.example.corebanking_service.exception.AppException;
import com.example.corebanking_service.exception.ErrorCode;
import com.example.corebanking_service.repository.CoreAccountRepo;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

import java.math.BigDecimal;

@RequiredArgsConstructor
@DubboService
public class CoreAccountDubboImpl implements AccountQueryService {
    private final CoreAccountRepo accountRepo;
    @Override
    public AccountDTO getAccountByNumber(String accountNumber) {
        CoreAccount account = accountRepo.findByAccountNumber(accountNumber);
        if(account!=null){
            AccountDTO accountDTO = AccountDTO.builder()
                    .accountNumber(account.getAccountNumber())
                    .cifCode(account.getCoreCustomer().getCifCode())
                    .accountType(account.getAccountType().name())
                    .balance(account.getBalance())
                    .status(account.getStatus())
                    .build();
            return accountDTO;
        }else return null;

    }

    @Override
    public BigDecimal getBalance(String accountNumber) {
        CoreAccount account = accountRepo.findByAccountNumber(accountNumber);
        if (account == null) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_EXIST);
        }
        return account.getBalance();
    }
}
