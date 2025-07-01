package com.example.loan_service.repository;

import com.example.loan_service.entity.LoanRejectionReason;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanRejectionReasonRepository extends JpaRepository<LoanRejectionReason,Long> {

}