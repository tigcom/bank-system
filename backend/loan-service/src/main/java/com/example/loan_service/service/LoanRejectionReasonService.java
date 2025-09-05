package com.example.loan_service.service;

import com.example.loan_service.entity.LoanRejectionReason;

import java.util.List;
import java.util.Optional;

public interface LoanRejectionReasonService {
    LoanRejectionReason save(LoanRejectionReason reason);

    Optional<LoanRejectionReason> findById(Long id);

    List<LoanRejectionReason> findAll();

    void deleteById(Long id);
}
