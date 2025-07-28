package com.example.loan_service.service;

import com.example.loan_service.entity.InfoIncome;

import java.util.List;
import java.util.Optional;

public interface InfoIncomeService {
    InfoIncome createInfoIncome(InfoIncome infoIncome);
    InfoIncome updateInfoIncome(InfoIncome infoIncome);
    Optional<InfoIncome> getById(Long infoId);
    List<InfoIncome> getByLoanId(Long loanId);
    void deleteInfoIncome(Long infoId);
}
