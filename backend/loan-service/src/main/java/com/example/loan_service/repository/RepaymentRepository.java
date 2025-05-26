package com.example.loan_service.repository;

import com.example.loan_service.entity.Loan;
import com.example.loan_service.entity.Repayment;
import com.example.loan_service.models.RepaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RepaymentRepository extends JpaRepository<Repayment, Long> {

    @Query("SELECT r FROM Repayment r WHERE r.loan.loanId = :loanId AND r.status <> 'PAID' ORDER BY r.dueDate ASC")
    List<Repayment> findUnpaidByLoanIdOrderByDueDate(@Param("loanId") Long loanId);

    @Query(value = "SELECT * FROM repayment WHERE loan_id = :loanId AND due_date >= CURDATE() AND YEAR(due_date) = YEAR(CURDATE()) ORDER BY due_date ASC LIMIT 1", nativeQuery = true)
    Repayment findNextRepaymentNative(@Param("loanId") Long loanId);

    @Query(value = "SELECT r.* FROM repayment r JOIN loan l ON r.loan_id = l.loan_id WHERE r.due_date >= CURDATE() AND l.customer_id = :customerId ORDER BY r.due_date ASC LIMIT 1", nativeQuery = true)
    Repayment findCurrentRepaymentByCustomerId(@Param("customerId") Long customerId);

    @Query("SELECT r FROM Repayment r WHERE r.loan.loanId = :loanId AND (r.status = 'PAID' OR r.status = 'PARTIAL')  order by r.dueDate desc")
    List<Repayment> findPaidOrPartialByLoanId(@Param("loanId") Long loanId);
}
