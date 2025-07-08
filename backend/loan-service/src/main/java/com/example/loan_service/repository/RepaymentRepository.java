package com.example.loan_service.repository;

import com.example.loan_service.entity.Loan;
import com.example.loan_service.entity.Repayment;
import com.example.loan_service.models.RepaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface RepaymentRepository extends JpaRepository<Repayment, Long> {

    @Query("SELECT r FROM Repayment r WHERE r.loan.loanId = :loanId AND r.status <> 'PAID' ORDER BY r.dueDate ASC")
    List<Repayment> findUnpaidByLoanIdOrderByDueDate(@Param("loanId") Long loanId);

    @Query(value = "SELECT * FROM repayment WHERE loan_id = :loanId AND due_date >= CURDATE() ORDER BY due_date ASC LIMIT 1", nativeQuery = true)
    Repayment findNextRepaymentNative(@Param("loanId") Long loanId);

    @Query(value = "SELECT r.* FROM repayment r JOIN loan l ON r.loan_id = l.loan_id WHERE r.due_date >= CURDATE() AND l.customer_id = :customerId ORDER BY r.due_date ASC LIMIT 1", nativeQuery = true)
    Repayment findCurrentRepaymentByCustomerId(@Param("customerId") Long customerId);

    @Query("SELECT r FROM Repayment r WHERE r.loan.customerId = :customerId AND (r.status != 'UNPAID')  order by r.dueDate desc")
    List<Repayment> findPaidOrPartialByLoanId(@Param("customerId") Long customerId);

    List<Repayment> findAllByLoan_LoanIdOrderByDueDateAsc(Long loanId);
    @Query("""
       SELECT r FROM Repayment r
        WHERE r.loan.loanId = :loanId
          AND r.status <> :late
          AND r.dueDate < CURRENT_DATE
        ORDER BY r.dueDate ASC
    """)
    List<Repayment> findOverdueByLoanId(
            @Param("loanId") Long loanId,
            @Param("late") RepaymentStatus late
    );

    @Query("""
       SELECT r FROM Repayment r
        WHERE r.loan.loanId = :loanId
          AND r.status = com.example.loan_service.models.RepaymentStatus.UNPAID
          AND r.dueDate BETWEEN CURRENT_DATE AND CURRENT_DATE + 3
        ORDER BY r.dueDate ASC
    """)
    List<Repayment> findUpcomingByLoanId(
            @Param("loanId") Long loanId
    );

    @Query("""
      SELECT COALESCE(
        SUM(
          CASE
            WHEN r.status = com.example.loan_service.models.RepaymentStatus.PAID THEN r.interest
            WHEN r.status = com.example.loan_service.models.RepaymentStatus.PARTIAL
              THEN CASE
                     WHEN r.paidAmount <= r.interest THEN r.paidAmount
                     ELSE r.interest
                   END
            ELSE 0
          END
        ), 0)
      FROM Repayment r
    """)
    BigDecimal sumProfit();

    @Query("""
      SELECT r.status, COUNT(r)
      FROM Repayment r
      GROUP BY r.status
    """)
    List<Object[]> countByStatus();
}
