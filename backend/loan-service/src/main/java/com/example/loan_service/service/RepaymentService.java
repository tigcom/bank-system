package com.example.loan_service.service;

import com.example.loan_service.entity.Loan;
import com.example.loan_service.entity.Repayment;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface RepaymentService {

    List<Repayment> generateRepaymentSchedule(Loan loan);

    List<Repayment> getRepaymentsByLoanId(Long loanId);
     List<Repayment> updateRepaymentSchedule(Loan loan, int startPeriodIndex, BigDecimal remainingPrincipal);
    Optional<Repayment> getRepaymentById(Long repaymentId);

    Repayment updateRepaymentStatus(Long repaymentId, String status);

    Repayment makeRepayment(Long repaymentId, java.math.BigDecimal amount);

    void deleteRepaymentsByLoanId(Long loanId);
}
