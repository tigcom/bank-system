package com.example.loan_service.service;

import com.example.loan_service.entity.Loan;
import com.example.loan_service.entity.Repayment;
import com.example.loan_service.models.RepaymentStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface RepaymentService {
    List<Repayment> getRepaymentNotPaid(Long loanId);
    List<Repayment> generateRepaymentSchedule(Loan loan);
    List<Repayment> getRepaymentsByLoanId(Long loanId);
    Optional<Repayment> getRepaymentById(Long repaymentId);
    Repayment updateRepaymentStatus(Long repaymentId, RepaymentStatus status);
    Repayment updateRepayment(Repayment repayment);
    Repayment makeRepayment(Long repaymentId, java.math.BigDecimal amount);
    void deleteRepaymentsByLoanId(Long loanId);
    Repayment getCurrentRepayment(Long loanId);
    List<Repayment> getHistoryRepayment(Long loanId);
}
