package com.example.loan_service.repository;

import com.example.loan_service.entity.Loan;
import com.example.loan_service.models.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findAllByStatusIs(LoanStatus status);
    List<Loan> findAllByStatusIsAndCustomerId(LoanStatus status,Long customerId);
    @Query("""
        SELECT COALESCE(SUM(l.amount), 0)
          FROM Loan l
         WHERE l.status IN :statuses
    """)
    BigDecimal sumAmountByStatuses(@Param("statuses") List<LoanStatus> statuses);
}
