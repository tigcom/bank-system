package com.example.loan_service.service.impl;

import com.example.loan_service.entity.LoanRejectionReason;
import com.example.loan_service.repository.LoanRejectionReasonRepository;
import com.example.loan_service.service.LoanRejectionReasonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanRejectionReasonServiceImpl implements LoanRejectionReasonService {

    private final LoanRejectionReasonRepository repository;

    @Override
    public LoanRejectionReason save(LoanRejectionReason rejectionReason) {
        log.info("SAVE_LOAN_REJECTION_REASON_START - reason: {}", rejectionReason.getReason());
        try {
            LoanRejectionReason saved = repository.save(rejectionReason);
            log.info("SAVE_LOAN_REJECTION_REASON_SUCCESS - id: {}", saved.getId());
            return saved;
        } catch (Exception e) {
            log.error("SAVE_LOAN_REJECTION_REASON_ERROR - reason: {}, error: {}", 
                      rejectionReason.getReason(), e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Optional<LoanRejectionReason> findById(Long id) {
        log.info("FIND_LOAN_REJECTION_REASON_START - id: {}", id);
        try {
            Optional<LoanRejectionReason> result = repository.findById(id);
            log.info("FIND_LOAN_REJECTION_REASON_SUCCESS - id: {}, found: {}", id, result.isPresent());
            return result;
        } catch (Exception e) {
            log.error("FIND_LOAN_REJECTION_REASON_ERROR - id: {}, error: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<LoanRejectionReason> findAll() {
        log.info("FIND_ALL_LOAN_REJECTION_REASONS_START");
        try {
            List<LoanRejectionReason> list = repository.findAll();
            log.info("FIND_ALL_LOAN_REJECTION_REASONS_SUCCESS - total: {}", list.size());
            return list;
        } catch (Exception e) {
            log.error("FIND_ALL_LOAN_REJECTION_REASONS_ERROR - error: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void deleteById(Long id) {
        log.info("DELETE_LOAN_REJECTION_REASON_START - id: {}", id);
        try {
            repository.deleteById(id);
            log.info("DELETE_LOAN_REJECTION_REASON_SUCCESS - id: {}", id);
        } catch (Exception e) {
            log.error("DELETE_LOAN_REJECTION_REASON_ERROR - id: {}, error: {}", id, e.getMessage(), e);
            throw e;
        }
    }
}
