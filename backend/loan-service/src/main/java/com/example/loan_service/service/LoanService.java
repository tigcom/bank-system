package com.example.loan_service.service;

import com.example.loan_service.entity.Loan;
import com.example.loan_service.entity.Repayment;

import java.util.List;
import java.util.Optional;

public interface LoanService {
    Loan createLoan(Loan loan);

    Loan updateLoan(Loan loan);

    Loan approveLoan(Long loanId);

    Optional<Loan> getLoanById(Long loanId);

    List<Loan> findAllLoan();

    List<Loan> getLoansByCustomerId(Long customerId);

    Optional<Loan> getLoanByCustomerId(Long customerId,Long loanId);

    List<Repayment> generateRepaymentSchedule(Loan loan);

    Loan updateLoanStatus(Long loanId, String status);

    void deleteLoan(Long loanId);
}
