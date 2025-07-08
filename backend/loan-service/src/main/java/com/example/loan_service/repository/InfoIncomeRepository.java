package com.example.loan_service.repository;

import com.example.loan_service.entity.InfoIncome;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InfoIncomeRepository extends JpaRepository<InfoIncome, Long> {
    Optional<InfoIncome> findByLoan_LoanId(Long loanId);
    List<InfoIncome> findAllByLoan_LoanId(Long loanId);
}
