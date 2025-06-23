package com.example.corebanking_service.service.impl;

import com.example.common_service.dto.*;
import com.example.common_service.dto.request.SavingUpdateRequest;
import com.example.common_service.dto.response.*;
import com.example.corebanking_service.entity.*;
import com.example.corebanking_service.exception.AppException;
import com.example.corebanking_service.exception.ErrorCode;
import com.example.corebanking_service.repository.*;
import com.example.corebanking_service.service.CoreAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoreAccountServiceImpl implements CoreAccountService {


    private final CoreCustomerRepo coreCustomerRepo;
    private final CoreAccountRepo coreAccountRepo;
    @Override
    public void createCoreAccount(CoreAccountRequest dto) {
            CoreAccount coreAccount = CoreAccount.builder()
                    .accountNumber(dto.getAccountNumber())
                    .accountType(dto.getAccountType())
                    .balance(dto.getBalance())
                    .status(dto.getStatus())
                    .coreCustomer(coreCustomerRepo.getCoreCustomerByCifCode(dto.getCifCode()))
                    .build();
            coreAccountRepo.save(coreAccount);
    }
    @Override
    public AccountSavingUpdateResponse updateBalanceSaving(String accountNumber, SavingUpdateRequest request) {
        log.info("Calling updateBalanceSaving with accountNumber: " + accountNumber);
        log.info("request: " + request);
        CoreAccount account = coreAccountRepo.findByAccountNumber(accountNumber);
        if (account == null) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_EXIST);
        }
        account.setBalance(request.getBalance());
        account.setStatus(request.getStatus());
        coreAccountRepo.save(account);
        return  AccountSavingUpdateResponse.builder()
                .accountNumber(accountNumber)
                .accountType(account.getAccountType())
                .accountType(account.getAccountType())
                .balance(account.getBalance())
                .status(account.getStatus())
                .build();
    }

    @Override
    public BalanceResponse getBalanceByAccountNumber(String accountNumber) {
        CoreAccount account = coreAccountRepo.findByAccountNumber(accountNumber);
        if (account == null) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_EXIST);
        }
        return BalanceResponse.builder()
                .accountNumber(accountNumber)
                .balance(account.getBalance())
                .build();
    }

    @Override
    public void updateStatus(CoreAccountUpdateStatusRequest statusRequest) {
        CoreAccount account = coreAccountRepo.findByAccountNumber(statusRequest.getAccountNumber());
        if (account == null) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_EXIST);
        }
        account.setStatus(statusRequest.getStatus());
        coreAccountRepo.save(account);
    }


}
