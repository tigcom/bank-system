package com.example.loan_service.service.impl;

import com.example.loan_service.entity.Loan;
import com.example.loan_service.models.LoanStatus;
import com.example.loan_service.repository.LoanRepository;
import com.example.loan_service.service.LoanService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanServiceImpl implements LoanService {

    private final LoanRepository loanRepository;

    @Override
    public Loan createLoan(Loan loan) {
        log.info("CREATE_LOAN_START - loan: {}", loan);
        try {
            loan.setStatus(LoanStatus.PENDING);
            loan.setCreatedAt(LocalDateTime.now());
            Loan saved = loanRepository.save(loan);
            log.info("CREATE_LOAN_SUCCESS - loanId: {}", saved.getLoanId());
            return saved;
        } catch (Exception e) {
            log.error("CREATE_LOAN_ERROR - error: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Loan updateLoan(Loan loan) {
        log.info("UPDATE_LOAN_START - loanId: {}, data: {}", loan.getLoanId(), loan);
        try {
            Loan updated = loanRepository.save(loan);
            log.info("UPDATE_LOAN_SUCCESS - loanId: {}", updated.getLoanId());
            return updated;
        } catch (Exception e) {
            log.error("UPDATE_LOAN_ERROR - loanId: {}, error: {}", loan.getLoanId(), e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<Loan> findAllLoan() {
        log.info("FIND_ALL_LOANS_START");
        try {
            List<Loan> list = loanRepository.findAll();
            log.info("FIND_ALL_LOANS_SUCCESS - total: {}", list.size());
            return list;
        } catch (Exception e) {
            log.error("FIND_ALL_LOANS_ERROR - error: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<Loan> getLoansApprove() {
        log.info("GET_APPROVED_LOANS_START");
        try {
            List<Loan> list = loanRepository.findAllByStatusIs(LoanStatus.APPROVED);
            log.info("GET_APPROVED_LOANS_SUCCESS - total: {}", list.size());
            return list;
        } catch (Exception e) {
            log.error("GET_APPROVED_LOANS_ERROR - error: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Optional<Loan> getLoanById(Long loanId) {
        log.info("GET_LOAN_BY_ID_START - loanId: {}", loanId);
        try {
            Optional<Loan> loan = loanRepository.findById(loanId);
            log.info("GET_LOAN_BY_ID_SUCCESS - found: {}", loan.isPresent());
            return loan;
        } catch (Exception e) {
            log.error("GET_LOAN_BY_ID_ERROR - loanId: {}, error: {}", loanId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void deleteLoan(Long loanId) {
        log.info("DELETE_LOAN_START - loanId: {}", loanId);
        try {
            Loan loan = loanRepository.findById(loanId)
                    .orElseThrow(() -> new EntityNotFoundException("Loan not found: " + loanId));
            if (!LoanStatus.PENDING.equals(loan.getStatus())) {
                log.warn("DELETE_LOAN_INVALID - loanId: {}, status: {}", loanId, loan.getStatus());
                throw new IllegalStateException("Only PENDING loans can be deleted");
            }
            loanRepository.deleteById(loanId);
            log.info("DELETE_LOAN_SUCCESS - loanId: {}", loanId);
        } catch (IllegalStateException e) {
            log.warn("DELETE_LOAN_WARN - loanId: {}, reason: {}", loanId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("DELETE_LOAN_ERROR - loanId: {}, error: {}", loanId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Loan approveLoan(Long loanId) {
        log.info("APPROVE_LOAN_START - loanId: {}", loanId);
        try {
            Loan loan = loanRepository.findById(loanId)
                    .orElseThrow(() -> new EntityNotFoundException("Loan not found: " + loanId));
            if (!LoanStatus.PENDING.equals(loan.getStatus())) {
                log.warn("APPROVE_LOAN_INVALID - loanId: {}, status: {}", loanId, loan.getStatus());
                throw new IllegalStateException("Loan is not in PENDING status");
            }
            loan.setStatus(LoanStatus.APPROVED);
            loan.setApprovedAt(LocalDateTime.now());
            Loan saved = loanRepository.save(loan);
            log.info("APPROVE_LOAN_SUCCESS - loanId: {}", saved.getLoanId());
            return saved;
        } catch (IllegalStateException e) {
            log.warn("APPROVE_LOAN_WARN - loanId: {}, reason: {}", loanId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("APPROVE_LOAN_ERROR - loanId: {}, error: {}", loanId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<Loan> getLoansByCustomerId(Long customerId) {
        log.info("GET_LOANS_BY_CUSTOMER_START - customerId: {}", customerId);
        try {
            List<Loan> list = loanRepository.findAll().stream()
                    .filter(loan -> loan.getCustomerId().equals(customerId))
                    .toList();
            log.info("GET_LOANS_BY_CUSTOMER_SUCCESS - customerId: {}, total: {}", customerId, list.size());
            return list;
        } catch (Exception e) {
            log.error("GET_LOANS_BY_CUSTOMER_ERROR - customerId: {}, error: {}", customerId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<Loan> getLoansApproveAndCustomerId(Long customerId) {
        log.info("GET_APPROVED_LOANS_BY_CUSTOMER_START - customerId: {}", customerId);
        try {
            List<Loan> list = loanRepository.findAllByStatusIsAndCustomerId(LoanStatus.APPROVED, customerId);
            log.info("GET_APPROVED_LOANS_BY_CUSTOMER_SUCCESS - customerId: {}, total: {}", customerId, list.size());
            return list;
        } catch (Exception e) {
            log.error("GET_APPROVED_LOANS_BY_CUSTOMER_ERROR - customerId: {}, error: {}", customerId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Loan rejectedLoan(Long loanId) {
        log.info("REJECT_LOAN_START - loanId: {}", loanId);
        try {
            Loan loan = loanRepository.findById(loanId)
                    .orElseThrow(() -> new EntityNotFoundException("Loan not found: " + loanId));
            if (!LoanStatus.PENDING.equals(loan.getStatus())) {
                log.warn("REJECT_LOAN_INVALID - loanId: {}, status: {}", loanId, loan.getStatus());
                throw new IllegalStateException("Loan is not in PENDING status");
            }
            loan.setStatus(LoanStatus.REJECTED);
            loan.setApprovedAt(LocalDateTime.now());
            Loan saved = loanRepository.save(loan);
            log.info("REJECT_LOAN_SUCCESS - loanId: {}", saved.getLoanId());
            return saved;
        } catch (IllegalStateException e) {
            log.warn("REJECT_LOAN_WARN - loanId: {}, reason: {}", loanId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("REJECT_LOAN_ERROR - loanId: {}, error: {}", loanId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Loan closedLoan(Long loanId) {
        log.info("CLOSE_LOAN_START - loanId: {}", loanId);
        try {
            Loan loan = loanRepository.findById(loanId)
                    .orElseThrow(() -> new EntityNotFoundException("Loan not found: " + loanId));
            if (!LoanStatus.APPROVED.equals(loan.getStatus())) {
                log.warn("CLOSE_LOAN_INVALID - loanId: {}, status: {}", loanId, loan.getStatus());
                throw new IllegalStateException("Loan is not in APPROVED status");
            }
            loan.setStatus(LoanStatus.CLOSED);
            loan.setApprovedAt(LocalDateTime.now());
            Loan saved = loanRepository.save(loan);
            log.info("CLOSE_LOAN_SUCCESS - loanId: {}", saved.getLoanId());
            return saved;
        } catch (IllegalStateException e) {
            log.warn("CLOSE_LOAN_WARN - loanId: {}, reason: {}", loanId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("CLOSE_LOAN_ERROR - loanId: {}, error: {}", loanId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public java.math.BigDecimal getTotalDisbursedSystem() {
        List<Loan> approved = loanRepository.findAllByStatusIs(com.example.loan_service.models.LoanStatus.APPROVED);
        List<Loan> closed = loanRepository.findAllByStatusIs(com.example.loan_service.models.LoanStatus.CLOSED);
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        for (Loan l : approved) total = total.add(l.getAmount());
        for (Loan l : closed) total = total.add(l.getAmount());
        return total;
    }
}
