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
import com.example.common_service.constant.NumberStatus;
@Slf4j
@Service
@RequiredArgsConstructor
public class CoreAccountServiceImpl implements CoreAccountService {


    private final CoreCustomerRepo coreCustomerRepo;
    private final CoreAccountRepo coreAccountRepo;
    private final CoreAccountNumberRepo coreAccountNumberRepo;
    @Override
    public void createCoreAccount(CoreAccountRequest dto) {
        log.info("=== [START] createCoreAccount ===");
        log.info("Request DTO nhận được: accountNumber={}, accountType={}, balance={}, status={}, cifCode={}",
                dto.getAccountNumber(), dto.getAccountType(), dto.getBalance(), dto.getStatus(), dto.getCifCode());

        // Tìm CoreAccountNumber theo accountNumber
        CoreAccountNumber coreAccountNumber = coreAccountNumberRepo.findByNumber(dto.getAccountNumber());
        log.info("CoreAccountNumber lấy được: id={}, number={}, status={}",
                coreAccountNumber.getAccountTypeCode(), coreAccountNumber.getNumber(), coreAccountNumber.getStatus());

        // Tìm CoreCustomer theo cifCode
        var coreCustomer = coreCustomerRepo.getCoreCustomerByCifCode(dto.getCifCode());
        log.info("CoreCustomer lấy được: id={}, cifCode={}, fullName={}",
                coreCustomer.getCifCode(), coreCustomer.getCifCode(), coreCustomer.getCifCode());

        // Tạo CoreAccount
        CoreAccount coreAccount = CoreAccount.builder()
                .coreAccountNumber(coreAccountNumber)
                .accountType(dto.getAccountType())
                .balance(dto.getBalance())
                .status(dto.getStatus())
                .coreCustomer(coreCustomer)
                .build();
        log.info("CoreAccount khởi tạo: accountType={}, balance={}, status={}, coreCustomerId={}",
                coreAccount.getAccountType(), coreAccount.getBalance(), coreAccount.getStatus(), coreAccount.getCoreCustomer().getCifCode());

        // Cập nhật trạng thái CoreAccountNumber
        coreAccountNumber.setStatus(NumberStatus.USED);
        coreAccountNumberRepo.save(coreAccountNumber);
        log.info("CoreAccountNumber đã cập nhật trạng thái sang USED và lưu thành công: id={}, number={}",
                coreAccountNumber.getAccountTypeCode(), coreAccountNumber.getNumber());

        // Lưu CoreAccount
        coreAccountRepo.save(coreAccount);
        log.info("CoreAccount đã lưu thành công: id={}", coreAccount.getId());

        log.info("=== [END] createCoreAccount ===");
    }

    @Override
    public AccountSavingUpdateResponse updateBalanceSaving(String accountNumber, SavingUpdateRequest request) {
        log.info("Calling updateBalanceSaving with accountNumber: " + accountNumber);
        log.info("request: " + request);
        accountNumber="1";
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
        log.info("Calling updateBalanceSaving with accountNumber: {}" + accountNumber);
        CoreAccount account = coreAccountRepo.findByAccountNumber(accountNumber);
        log.info("account: {}" + account.getCoreAccountNumber().getNumber());
        if (account == null) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_EXIST);
        }
        return BalanceResponse.builder()
                .accountNumber(accountNumber)
                .balance(account.getBalance())
                .build();
    }

    @Override
    public void updateCoreAccount(CoreAccountRequest dto) {
        log.info("UPDATE_CORE_ACCOUNT_START - accountNumber: {}, balance: {}, status: {}", dto.getAccountNumber(), dto.getBalance(),dto.getStatus());
        
        // Tìm account hiện có
        CoreAccount existingAccount = coreAccountRepo.findByAccountNumber(dto.getAccountNumber());
        if (existingAccount == null) {
            log.error("UPDATE_CORE_ACCOUNT_ERROR - Account not found: {}", dto.getAccountNumber());
            throw new AppException(ErrorCode.ACCOUNT_NOT_EXIST);
        }
        
        // Cập nhật thông tin account
        existingAccount.setBalance(dto.getBalance());
        existingAccount.setStatus(dto.getStatus());
        if (dto.getAccountType() != null) {
            existingAccount.setAccountType(dto.getAccountType());
        }
        
        coreAccountRepo.save(existingAccount);
        log.info("UPDATE_CORE_ACCOUNT_SUCCESS - accountNumber: {}, newBalance: {}", dto.getAccountNumber(), dto.getBalance());
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
