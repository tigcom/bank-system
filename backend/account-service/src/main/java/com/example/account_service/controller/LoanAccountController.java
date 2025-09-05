package com.example.account_service.controller;

import com.example.account_service.entity.LoanAccount;
import com.example.common_service.constant.AccountStatus;
import com.example.account_service.service.LoanAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loan-accounts")
@RequiredArgsConstructor
public class LoanAccountController {

    private final LoanAccountService loanAccountService;

    @PostMapping
    public ResponseEntity<LoanAccount> createLoanAccount(@RequestBody LoanAccount loanAccount) {
        LoanAccount created = loanAccountService.createLoanAccount(loanAccount);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<LoanAccount> getByAccountNumber(@PathVariable String accountNumber) {
        LoanAccount account = loanAccountService.getByAccountNumber(accountNumber);
        if (account == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(account);
    }

    @GetMapping("/cif/{cifCode}")
    public ResponseEntity<List<LoanAccount>> getByCifCode(@PathVariable String cifCode) {
        return ResponseEntity.ok(loanAccountService.getByCifCode(cifCode));
    }

    @GetMapping("/cif/{cifCode}/status/{status}")
    public ResponseEntity<List<LoanAccount>> getByCifCodeAndStatus(@PathVariable String cifCode,
                                                                   @PathVariable AccountStatus status) {
        return ResponseEntity.ok(loanAccountService.getByCifCodeAndStatus(cifCode, status));
    }

    @PutMapping("/{accountNumber}/debt")
    public ResponseEntity<Void> updateRemainingDebt(@PathVariable String accountNumber,
                                                    @RequestBody LoanAccount updatedLoan) {
        loanAccountService.updateRemainingDebt(accountNumber, updatedLoan);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{accountNumber}/close")
    public ResponseEntity<Void> closeLoan(@PathVariable String accountNumber) {
        loanAccountService.closeLoan(accountNumber);
        return ResponseEntity.ok().build();
    }
}
