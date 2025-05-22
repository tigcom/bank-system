package com.example.loan_service.handler;


import com.example.loan_service.entity.Loan;
import com.example.loan_service.entity.Repayment;
import com.example.loan_service.service.LoanService;
import com.example.loan_service.service.RepaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LoanHandler {

    private final LoanService loanService;
    private final RepaymentService repaymentService;

    public Loan createLoan(Loan loan) {
        return loanService.createLoan(loan);
    }

    public Loan updateLoan(Loan loan) {
        return loanService.updateLoan(loan);
    }

    public Loan approveLoan(Long loanId) {
        Loan loan =  new Loan();
        try {
            loan = loanService.approveLoan(loanId);
            repaymentService.generateRepaymentSchedule(loan);
        }catch (Exception e) {
            e.printStackTrace();
        }
        return loan;
    }

    public Optional<Loan> getLoanById(Long loanId) {
        return loanService.getLoanById(loanId);
    }

    public List<Loan> getLoansByCustomerId(Long customerId) {
        return loanService.getLoansByCustomerId(customerId);
    }

    public Optional<Loan> getLoanByCustomerAndLoanId(Long customerId, Long loanId) {
        return loanService.getLoanByCustomerId(customerId, loanId);
    }

    public Loan updateLoanStatus(Long loanId, String status) {
        return loanService.updateLoanStatus(loanId, status);
    }

    public void deleteLoan(Long loanId) {
        loanService.deleteLoan(loanId);
    }

    public List<Repayment> generateRepaymentSchedule(Loan loan) {
        return repaymentService.generateRepaymentSchedule(loan);
    }

    public List<Repayment> getRepaymentsByLoanId(Long loanId) {
        return repaymentService.getRepaymentsByLoanId(loanId);
    }

    public Repayment makeRepayment(Long repaymentId, java.math.BigDecimal amount) {
        return repaymentService.makeRepayment(repaymentId, amount);
    }

    public Optional<Repayment> getRepaymentById(Long repaymentId) {
        return repaymentService.getRepaymentById(repaymentId);
    }

    public Repayment updateRepaymentStatus(Long repaymentId, String status) {
        return repaymentService.updateRepaymentStatus(repaymentId, status);
    }

    public List<Repayment> updateRepaymentSchedule(Loan loan, int startPeriodIndex, BigDecimal remainingPrincipal) {
        return repaymentService.updateRepaymentSchedule(loan, startPeriodIndex, remainingPrincipal);
    }

    public void deleteRepaymentsByLoanId(Long loanId) {
        repaymentService.deleteRepaymentsByLoanId(loanId);
    }
}
