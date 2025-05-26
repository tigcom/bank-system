package com.example.loan_service.handler;


import com.example.loan_service.entity.Loan;
import com.example.loan_service.entity.Repayment;
import com.example.loan_service.models.RepaymentStatus;
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

    public Loan closedLoan(Long loanId) {
        Loan loan =  new Loan();
        try {
            loan = loanService.closedLoan(loanId);
        }catch (Exception e) {
            e.printStackTrace();
        }
        return loan;
    }

    public Loan rejectedLoan(Long loanId) {
        Loan loan =  new Loan();
        try {
            loan = loanService.rejectedLoan(loanId);
        }catch (Exception e) {
            e.printStackTrace();
        }
        return loan;
    }

    public void deleteLoan(Long loanId) {
        loanService.deleteLoan(loanId);
    }

    public List<Repayment> getRepaymentsByLoanId(Long loanId) {
        return repaymentService.getRepaymentsByLoanId(loanId);
    }

    public Repayment makeRepayment(Long repaymentId, java.math.BigDecimal amount) {
        return repaymentService.makeRepayment(repaymentId, amount);
    }

    public List<Repayment> getHistory(Long loanId) { return repaymentService.getHistoryRepayment(loanId);}

    public Repayment getCurrentRepayment(Long loanId) { return repaymentService.getCurrentRepayment(loanId);}

    public Optional<Repayment> getRepaymentById(Long repaymentId) {
        return repaymentService.getRepaymentById(repaymentId);
    }

    public Repayment unpaidRepayment(Long repaymentId) {
        return repaymentService.updateRepaymentStatus(repaymentId, RepaymentStatus.UNPAID);
    }

    public Repayment lateRepayment(Long repaymentId) {
        return repaymentService.updateRepaymentStatus(repaymentId, RepaymentStatus.LATE);
    }

    public void deleteRepaymentsByLoanId(Long loanId) {
        repaymentService.deleteRepaymentsByLoanId(loanId);
    }
}
