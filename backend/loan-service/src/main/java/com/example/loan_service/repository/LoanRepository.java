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
    
    @Query("SELECT l FROM Loan l WHERE l.status = :status ORDER BY l.createdAt DESC")
    List<Loan> findAllByStatusOrderByCreatedAtDesc(@Param("status") LoanStatus status);
    @Query("""
        SELECT COALESCE(SUM(l.amount), 0)
          FROM Loan l
         WHERE l.status IN :statuses
    """)
    BigDecimal sumAmountByStatuses(@Param("statuses") List<LoanStatus> statuses);

    @Query(value = "SELECT \n" +
            "    COALESCE(SUM(r.principal + r.interest - r.paid_amount), 0) AS total_outstanding\n" +
            "FROM loan l\n" +
            "JOIN repayment r ON l.loan_id = r.loan_id\n" +
            "WHERE l.customer_id = :customerId \n" +
            "  AND l.status ='approved';\n",nativeQuery = true)
    BigDecimal getTotalOutstanding(@Param("customerId")  Long customerId);

    @Query(value = "SELECT \n" +
            "    COALESCE(SUM(r.principal + r.interest - r.paid_amount), 0) AS total_outstanding\n" +
            "FROM loan l\n" +
            "JOIN repayment r ON l.loan_id = r.loan_id\n" +
            "WHERE l.loan_id = :loanId",nativeQuery = true)
    BigDecimal getTotalOutstandingByLoan(@Param("loanId")  Long loanId);

    @Query(value = "select COALESCE(sum(l.amount )) from loan l where customer_id = :customerId and status in ( 'APPROVED','CLOSED')",nativeQuery = true)
    BigDecimal getTotalBorrowed (@Param("customerId") Long customerId);
}
