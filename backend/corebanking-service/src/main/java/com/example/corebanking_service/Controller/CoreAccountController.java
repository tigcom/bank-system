package com.example.corebanking_service.controller;
import com.example.common_service.dto.*;
import com.example.common_service.dto.request.SavingUpdateRequest;
import com.example.common_service.dto.response.*;
import com.example.corebanking_service.entity.CoreAccountNumber;
import com.example.corebanking_service.repository.CoreAccountRepo;
import com.example.corebanking_service.service.CoreAccountService;
import com.example.corebanking_service.service.CoreBankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor

public class CoreAccountController {
    private final CoreAccountService coreAccountService;
    private final CoreAccountRepo coreAccountRepo;
    private final CoreBankingService coreBankingService;
    @PostMapping("/save-account")
    public void createAccount(@RequestBody CoreAccountRequest accountRequest) {
        coreAccountService.createCoreAccount(accountRequest);
    }
    @PostMapping("/update-account-status")
    public void updateStatus(@RequestBody CoreAccountUpdateStatusRequest statusRequest) {
        coreAccountService.updateStatus(statusRequest);
    }
    @GetMapping("/get-balance-by-accountNumber/{accountNumber}")
    public BalanceResponse getBalanceByAccountNumber(@PathVariable String accountNumber) {
        return coreAccountService.getBalanceByAccountNumber(accountNumber);
    }
    @PutMapping("/update-balance-account-saving/{accountNumber}")
    public AccountSavingUpdateResponse updateBalance(@PathVariable String accountNumber, @RequestBody SavingUpdateRequest request)
    {
        return coreAccountService.updateBalanceSaving(accountNumber,request);
    }
    @GetMapping("/account-numbers/generate/{bankCode}")
    public List<CoreAccountNumber> generateAccountNumber (@PathVariable String bankCode){
        return coreBankingService.generateAccountNumber(bankCode);
    }
    @GetMapping("/getAccountNumber/{typeAccount}")
    public String getAccountNumber (@PathVariable String typeAccount){
        return coreBankingService.getAccountNumber(typeAccount);
    }


}