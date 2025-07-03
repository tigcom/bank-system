package com.example.corebanking_service.Controller;
import com.example.common_service.dto.*;
import com.example.common_service.dto.request.SavingUpdateRequest;
import com.example.common_service.dto.response.*;
import com.example.corebanking_service.repository.CoreAccountRepo;
import com.example.corebanking_service.service.CoreAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor

public class CoreAccountController {
    private final CoreAccountService coreAccountService;
    private final CoreAccountRepo coreAccountRepo;

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

}

