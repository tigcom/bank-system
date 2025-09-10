package com.example.account_service.service;

import com.example.account_service.entity.LoanAccount;
import com.example.common_service.constant.AccountStatus;

import java.util.List;
import java.util.Optional;

public interface LoanAccountService {

    LoanAccount createLoanAccount(LoanAccount loanAccount);

    Optional<LoanAccount> getById(String id);

    LoanAccount getByAccountNumber(String accountNumber);

    List<LoanAccount> getByCifCode(String cifCode);

    List<LoanAccount> getByCifCodeAndStatus(String cifCode, AccountStatus status);

    boolean existsByAccountNumber(String accountNumber);

    void updateRemainingDebt(String accountNumber, LoanAccount updatedLoan);

    void closeLoan(String accountNumber);
}
