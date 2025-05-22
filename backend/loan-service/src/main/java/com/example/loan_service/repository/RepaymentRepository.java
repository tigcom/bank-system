package com.example.loan_service.repository;

import com.example.loan_service.entity.Loan;
import com.example.loan_service.entity.Repayment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepaymentRepository extends JpaRepository<Repayment, Long> {
}
