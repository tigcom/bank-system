package com.example.corebanking_service.service.impl;

import com.example.common_service.services.account.CoreQueryService;
import com.example.corebanking_service.entity.CoreAccount;
import com.example.corebanking_service.exception.AppException;
import com.example.corebanking_service.exception.ErrorCode;
import com.example.corebanking_service.repository.CoreAccountRepo;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

import java.math.BigDecimal;

@RequiredArgsConstructor
@DubboService
public class CoreAccountDubboImpl implements CoreQueryService {
    private final CoreAccountRepo accountRepo;

    @Override
    public BigDecimal getBalance(String accountNumber) {
        CoreAccount account = accountRepo.findByAccountNumber(accountNumber);
        if (account == null) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_EXIST);
        }
        return account.getBalance();
    }
}
