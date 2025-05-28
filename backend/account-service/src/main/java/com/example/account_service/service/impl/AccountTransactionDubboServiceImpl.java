package com.example.account_service.service.impl;

import com.example.account_service.entity.Account;
import com.example.account_service.repository.AccountRepository;
import com.example.common_service.dto.AccountDTO;
import com.example.common_service.services.account.AccountQueryService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;


@DubboService
@RequiredArgsConstructor
public class AccountTransactionDubboServiceImpl implements AccountQueryService {

    private final AccountRepository accountRepository;
    @Override
    public AccountDTO getAccountByAccountNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber);
        if(account!=null){
            AccountDTO accountDTO = AccountDTO.builder()
                    .accountNumber(account.getAccountNumber())
                    .cifCode(account.getCifCode())
                    .accountType(account.getAccountType().name())
                    .status(account.getStatus().name())
                    .build();
            return accountDTO;
        }else return null;
    }


}
