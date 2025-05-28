package com.example.corebanking_service.controller;

import com.example.common_service.dto.CartTypeDTO;
import com.example.common_service.dto.CorePaymentAccountDTO;
import com.example.common_service.dto.coreCreditAccountDTO;
import com.example.common_service.dto.coreSavingAccountDTO;
import com.example.common_service.dto.response.AccountSummaryDTO;
import com.example.corebanking_service.repository.CoreAccountRepo;
import com.example.corebanking_service.service.CoreAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor

public class CoreAccountController {
    private final CoreAccountService coreAccountService;
    private final CoreAccountRepo coreAccountRepo;

    @PostMapping("/create-payment-account")
    public void createPaymentAccount(@RequestBody CorePaymentAccountDTO corePaymentAccountDTO) {
        coreAccountService.createCoreAccountPayment(corePaymentAccountDTO);
    }
    @PostMapping("/create-savings-account")
    public void createSavingsAccount(@RequestBody coreSavingAccountDTO coreSavingAccountDTO) {
        coreAccountService.createCoreAccountSaving(coreSavingAccountDTO);
    }
    @GetMapping("/get-cart-type/{id}")
    public CartTypeDTO  getCardInfoByID(@PathVariable String id) {
         return  coreAccountService.getCartTypebyID(id);
    }
    @PostMapping("/create-credit-account")
    public void createCreditAccount(@RequestBody coreCreditAccountDTO coreCreditAccountDTO) {
        coreAccountService.createCoreAccountCredit(coreCreditAccountDTO);
    }
    @GetMapping("/get-all-account-by-cifcode/{id}")
    public List<AccountSummaryDTO> getAllAccountByCifCode(@PathVariable String id) {
        List<AccountSummaryDTO> list = coreAccountService.getAllAccountsByCif(id);
        return list;

    }
}

