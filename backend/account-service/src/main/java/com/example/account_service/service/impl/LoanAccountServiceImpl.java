package com.example.account_service.service.impl;

import com.example.account_service.entity.LoanAccount;
import com.example.common_service.constant.AccountStatus;
import com.example.account_service.repository.LoanAccountRepository;
import com.example.account_service.service.LoanAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class LoanAccountServiceImpl implements LoanAccountService {

    private final LoanAccountRepository loanAccountRepository;

    @Override
    public LoanAccount createLoanAccount(LoanAccount loanAccount) {
        return loanAccountRepository.save(loanAccount);
    }

    @Override
    public Optional<LoanAccount> getById(String id) {
        return loanAccountRepository.findById(id);
    }

    @Override
    public LoanAccount getByAccountNumber(String accountNumber) {
        return loanAccountRepository.findByAccountNumber(accountNumber);
    }

    @Override
    public List<LoanAccount> getByCifCode(String cifCode) {
        return loanAccountRepository.findByCifCode(cifCode);
    }

    @Override
    public List<LoanAccount> getByCifCodeAndStatus(String cifCode, AccountStatus status) {
        return loanAccountRepository.findByCifCodeAndStatus(cifCode, status);
    }

    @Override
    public boolean existsByAccountNumber(String accountNumber) {
        return loanAccountRepository.existsByAccountNumber(accountNumber);
    }

    @Override
    public void updateRemainingDebt(String accountNumber, LoanAccount updatedLoan) {
        LoanAccount existing = loanAccountRepository.findByAccountNumber(accountNumber);
        if (existing != null) {
            existing.setOutstandingDebt(updatedLoan.getOutstandingDebt());
            loanAccountRepository.save(existing);
        }
    }

    @Override
    public void closeLoan(String accountNumber) {
        LoanAccount existing = loanAccountRepository.findByAccountNumber(accountNumber);
        if (existing != null) {
            existing.setStatus(AccountStatus.CLOSED);
            loanAccountRepository.save(existing);
        }
    }
}