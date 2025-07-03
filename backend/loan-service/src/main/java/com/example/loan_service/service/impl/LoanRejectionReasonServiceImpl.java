package com.example.loan_service.service.impl;

import com.example.loan_service.entity.LoanRejectionReason;
import com.example.loan_service.repository.LoanRejectionReasonRepository;
import com.example.loan_service.service.LoanRejectionReasonService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class LoanRejectionReasonServiceImpl implements LoanRejectionReasonService {

    private final LoanRejectionReasonRepository repository;

    public LoanRejectionReasonServiceImpl(LoanRejectionReasonRepository repository) {
        this.repository = repository;
    }

    @Override
    public LoanRejectionReason save(LoanRejectionReason reason) {
        return repository.save(reason);
    }

    @Override
    public Optional<LoanRejectionReason> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<LoanRejectionReason> findAll() {
        return repository.findAll();
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}