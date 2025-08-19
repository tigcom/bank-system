package com.example.loan_service.service.impl;

import com.example.loan_service.entity.Loan;
import com.example.loan_service.models.LoanStatus;
import com.example.loan_service.repository.LoanRepository;
import com.example.loan_service.service.LoanService;
import com.example.loan_service.service.LoanMetricsService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;
import io.micrometer.core.instrument.Timer;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanServiceImpl implements LoanService {

    private final LoanRepository loanRepository;
    private final LoanMetricsService metricsService;

    @Override
    @CacheEvict(value = {"loanById", "allLoans"}, allEntries = true)
    @RateLimiter(name = "loanCreation", fallbackMethod = "createLoanFallback")
    public Loan createLoan(Loan loan) {
        log.info("CREATE_LOAN_START - loan: {}", loan);
        Timer.Sample timer = metricsService.startLoanApplicationProcessing();
        
        try {
            // Increment loan applications counter
            metricsService.incrementLoanApplications();
            
            // Record loan amount and term for distribution
            if (loan.getAmount() != null) {
                metricsService.recordLoanAmount(loan.getAmount().doubleValue());
            }
            if (loan.getTermMonths() != null) {
                metricsService.recordLoanTerm(loan.getTermMonths());
            }
            
            loan.setStatus(LoanStatus.PENDING);
            loan.setCreatedAt(LocalDateTime.now());
            Loan saved = loanRepository.save(loan);
            
            log.info("CREATE_LOAN_SUCCESS - loanId: {}", saved.getLoanId());
            return saved;
        } catch (Exception e) {
            log.error("CREATE_LOAN_ERROR - error: {}", e.getMessage(), e);
            throw e;
        } finally {
            metricsService.stopLoanApplicationProcessing(timer);
        }
    }

    public Loan createLoanFallback(Loan loan, Throwable t) {
        log.warn("CREATE_LOAN_FALLBACK - loan: {}, error: {}", loan, t.getMessage());
        throw new RuntimeException("Loan creation is temporarily unavailable: " + t.getMessage(), t);
    }

    @Override
    @CachePut(value = "loanById", key = "#loan.loanId")
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

    public Loan updateLoanFallback(Loan loan, Throwable t) {
        log.warn("UPDATE_LOAN_FALLBACK - loanId: {}, error: {}", loan.getLoanId(), t.getMessage());
        throw new RuntimeException("Loan update is temporarily unavailable: " + t.getMessage(), t);
    }

    @Override
    @Cacheable(value = "allLoans", key = "'allLoans'")
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

    public List<Loan> findAllLoanFallback(Throwable t) {
        log.warn("FIND_ALL_LOANS_FALLBACK - error: {}", t.getMessage());
        throw new RuntimeException("Database operation is temporarily unavailable: " + t.getMessage(), t);
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
    @Transactional
    @Cacheable(value = "loanById", key = "#loanId")
    public Loan getLoanById(Long loanId) {
        log.info("GET_LOAN_BY_ID_START - loanId: {}", loanId);
        try {
            Loan loan = loanRepository.findById(loanId).orElse(null);
            log.info("GET_LOAN_BY_ID_SUCCESS - found: {}", loan.getLoanId());
            return loan;
        } catch (Exception e) {
            log.error("GET_LOAN_BY_ID_ERROR - loanId: {}, error: {}", loanId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @CacheEvict(value = "loanById", key = "#loanId")
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
    @CacheEvict(value = {"loanById", "allLoans"}, allEntries = true)
    @RateLimiter(name = "loanApproval", fallbackMethod = "approveLoanFallback")
    public Loan approveLoan(Loan loan) {
        log.info("APPROVE_LOAN_START - loanId: {}", loan.getLoanId());
        
        try {
            if (!LoanStatus.PENDING.equals(loan.getStatus())) {
                log.warn("APPROVE_LOAN_INVALID - loanId: {}, status: {}",  loan.getLoanId(), loan.getStatus());
                throw new IllegalStateException("Loan is not in PENDING status");
            }
            
            loan.setStatus(LoanStatus.APPROVED);
            loan.setApprovedAt(LocalDateTime.now());
            Loan saved = loanRepository.save(loan);
            
            // Increment loan approvals counter
            metricsService.incrementLoanApprovals();
            
            log.info("APPROVE_LOAN_SUCCESS - loanId: {}", saved.getLoanId());
            return saved;
        } catch (IllegalStateException e) {
            log.warn("APPROVE_LOAN_WARN - loanId: {}, reason: {}",  loan.getLoanId(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("APPROVE_LOAN_ERROR - loanId: {}, error: {}",  loan.getLoanId(), e.getMessage(), e);
            throw e;
        }
    }

    public Loan approveLoanFallback(Loan loan, Throwable t) {
        log.warn("APPROVE_LOAN_FALLBACK - loanId: {}, error: {}", loan.getLoanId(), t.getMessage());
        throw new RuntimeException("Loan approval is temporarily unavailable: " + t.getMessage(), t);
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
    @CacheEvict(value = {"loanById", "allLoans"}, allEntries = true)
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
            
            // Increment loan rejections counter
            metricsService.incrementLoanRejections();
            
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
    @CacheEvict(value = {"loanById", "allLoans"}, allEntries = true)
    public Loan closedLoan(Long loanId) {
        log.info("CLOSE_LOAN_START - loanId: {}", loanId);
        Timer.Sample timer = metricsService.startLoanDisbursement();
        
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
            
            // Increment loan disbursements counter
            metricsService.incrementLoanDisbursements();
            
            log.info("CLOSE_LOAN_SUCCESS - loanId: {}", saved.getLoanId());
            return saved;
        } catch (IllegalStateException e) {
            log.warn("CLOSE_LOAN_WARN - loanId: {}, reason: {}", loanId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("CLOSE_LOAN_ERROR - loanId: {}, error: {}", loanId, e.getMessage(), e);
            throw e;
        } finally {
            metricsService.stopLoanDisbursement(timer);
        }
    }

    @Override
    public BigDecimal getTotalDisbursedSystem() {
        log.info("GET_TOTAL_DISBURSED_SYSTEM_START");
        try {
            BigDecimal total = loanRepository.sumAmountByStatuses(
                    List.of(LoanStatus.APPROVED, LoanStatus.CLOSED)
            );
            log.info("GET_TOTAL_DISBURSED_SYSTEM_SUCCESS - total: {}", total);
            return total;
        } catch (Exception e) {
            log.error("GET_TOTAL_DISBURSED_SYSTEM_ERROR - error: {}", e.getMessage(), e);
            throw e;
        }
    }
}
