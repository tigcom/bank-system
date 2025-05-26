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
    List<Loan> getLoansApprove();
    List<Loan> getLoansByCustomerId(Long customerId);

    Loan rejectedLoan(Long loanId);

    Loan closedLoan(Long loanId);

    void deleteLoan(Long loanId);
}
