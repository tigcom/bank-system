package com.example.loan_service.repository;

import com.example.loan_service.entity.Loan;
import com.example.loan_service.models.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findAllByStatusIs(LoanStatus status);
}
