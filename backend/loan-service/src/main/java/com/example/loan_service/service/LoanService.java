package com.example.loan_service.service;

import com.example.loan_service.entity.Loan;

import java.math.BigDecimal;
import java.util.List;

public interface LoanService {
    Loan createLoan(Loan loan);
    BigDecimal getTotalBorrowed (Long customerId);
    Loan updateLoan(Loan loan);
    Loan approveLoan(Loan loan);
    Loan getLoanById(Long loanId);
    List<Loan> findAllLoan();
    List<Loan> getLoansApprove();
    List<Loan> getLoansByCustomerId(Long customerId);
    List<Loan>getLoansApproveAndCustomerId(Long customerId);


    BigDecimal getTotalOutstanding(Long customerId);

    BigDecimal getTotalOutstandingByLoan(Long loanId);

    Loan rejectedLoan(Long loanId);

    Loan closedLoan(Long loanId);
    Loan cancelledLoan(Long loanId);
    void deleteLoan(Long loanId);
    java.math.BigDecimal getTotalDisbursedSystem();


    List<Loan> getPendingLoans();
    java.math.BigDecimal getTotalRecoveredSystem();
}