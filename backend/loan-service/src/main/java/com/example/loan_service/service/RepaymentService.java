package com.example.loan_service.service;

import com.example.loan_service.entity.Loan;
import com.example.loan_service.entity.Repayment;
import com.example.loan_service.models.RepaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
    Repayment getCurrentRepayment(Long customerId);
    Repayment getCurrentRepaymentbyLoanId(Long loanId);
    List<Repayment> getHistoryRepayment(Long loanId);
    List<Repayment> getOverdueRepayments(Long loanId);
    List<Repayment> getUpcomingRepayments(Long loanId);
    Boolean checkLastMonthRepayment(Repayment repayment);
    Integer checkPreviousMonthLate(Long repaymentId);
    Boolean shouldCloseLoan(Long loanId);

    java.math.BigDecimal getTotalCollectedSystem();

    java.math.BigDecimal getTotalProfitSystem();

    Map<String, Long> getRepaymentStats();
    BigDecimal getOutstandingDebtByLoanId (Long loanId);
    
    /**
     * Lấy tất cả các kỳ trả nợ đã quá hạn
     */
    List<Repayment> getAllOverdueRepayments();
    List<Repayment> findAfter3DaysNative();
}
