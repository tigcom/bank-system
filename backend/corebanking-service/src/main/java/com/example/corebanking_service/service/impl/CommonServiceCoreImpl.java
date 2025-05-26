package com.example.corebanking_service.service.impl;

import com.example.common_service.constant.AccountStatus;
import com.example.common_service.dto.CorePaymentAccountDTO;
import com.example.common_service.dto.coreSavingAccountDTO;
import com.example.common_service.services.CommonServiceCore;
import com.example.corebanking_service.entity.CoreAccount;
import com.example.corebanking_service.entity.CoreSavingsAccount;
import com.example.corebanking_service.repository.CoreAccountRepo;
import com.example.corebanking_service.repository.CoreAccountSavingRepo;
import com.example.corebanking_service.repository.CoreCustomerRepo;
import com.example.corebanking_service.repository.CoreTermRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@DubboService(interfaceClass = CommonServiceCore.class)
@RequiredArgsConstructor
public class CommonServiceCoreImpl implements CommonServiceCore {
    private final CoreCustomerRepo coreCustomerRepo;
    private final CoreAccountRepo coreAccountRepo;
    private final CoreAccountSavingRepo coreAccountSavingRepo;
    private final CoreTermRepo coreTermRepo;
    @Override
    public void createCoreAccountPayment(CorePaymentAccountDTO dto) {
            CoreAccount coreAccount = CoreAccount.builder()
                    .accountNumber(dto.getAccountNumber())
                    .accountType(dto.getAccountType())
                    .balance(BigDecimal.ZERO)
                    .status(dto.getStatus())
                    .openedDate(LocalDate.now())
                    .coreCustomer(coreCustomerRepo.getCoreCustomerByCifCode(dto.getCifCode()))
                    .build();
            log.info("createCoreAccountPayment:{}", coreAccount);
            coreAccountRepo.save(coreAccount);
    }

    @Override
    public void createCoreAccountSaving(coreSavingAccountDTO dto) {
        CoreSavingsAccount coreSavingsAccount=CoreSavingsAccount.builder()
                .accountNumber(dto.getAccountNumber())
                .accountType(dto.getAccountType())
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .openedDate(LocalDate.now())
                .coreCustomer(coreCustomerRepo.getCoreCustomerByCifCode(dto.getCifCode()))
                .initialDeposit(dto.getInitialDeposit())
                .coreTerm(coreTermRepo.getCoreTermsByTermValueMonths(dto.getTerm()))
                .build();
        coreAccountSavingRepo.save(coreSavingsAccount);
    }
}
