package com.example.account_service.service.impl;

import com.example.account_service.entity.Account;
import com.example.account_service.repository.AccountRepository;
import com.example.common_service.dto.AccountDTO;
import com.example.common_service.services.customer.CustomerCommonService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;
import java.util.stream.Collectors;

@DubboService
@RequiredArgsConstructor
public class AccountCustomerDubboServiceImpl implements CustomerCommonService {

    private final AccountRepository accountRepository;

    @Override
    public List<AccountDTO> getAccountsByCifCode(String cifCode) {
        List<Account> accounts = accountRepository.findByCifCode(cifCode);
        return accounts.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private AccountDTO mapToDto(Account account) {
        return AccountDTO.builder()
                .accountNumber(account.getAccountNumber())
                .cifCode(account.getCifCode())
                .accountType(account.getAccountType().toString())
                .status(account.getStatus().toString())
                .build();
    }

}
