package com.example.loan_service.service.impl;

import com.example.loan_service.entity.Loan;
import com.example.loan_service.entity.Repayment;
import com.example.loan_service.mapper.LoanMapper;
import com.example.loan_service.models.LoanStatus;
import com.example.loan_service.models.RepaymentStatus;
import com.example.loan_service.repository.LoanRepository;
import com.example.loan_service.repository.RepaymentRepository;
import com.example.loan_service.service.CoreBankingClient;
import com.example.loan_service.service.LoanService;
import com.example.loan_service.service.RepaymentService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepaymentServiceImpl implements RepaymentService {

    private final RepaymentRepository repaymentRepository;
    private final LoanRepository loanRepository;
    private final LoanService loanService;
    private final LoanMapper loanMapper;
    private final CoreBankingClient bankingClient;

    @Override
    public List<Repayment> getRepaymentNotPaid(Long loanId) {
        log.info("GET_UNPAID_REPAYMENTS_START - loanId: {}", loanId);
        try {
            List<Repayment> list = repaymentRepository.findUnpaidByLoanIdOrderByDueDate(loanId);
            log.info("GET_UNPAID_REPAYMENTS_SUCCESS - loanId: {}, count: {}", loanId, list.size());
            return list;
        } catch (Exception e) {
            log.error("GET_UNPAID_REPAYMENTS_ERROR - loanId: {}, error: {}", loanId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<Repayment> generateRepaymentSchedule(Loan loan) {
        log.info("GENERATE_REPAYMENT_SCHEDULE_START - loanId: {}", loan.getLoanId());
        try {
            List<Repayment> repayments = new ArrayList<>();
            BigDecimal monthlyInterestRate = loan.getInterestRate()
                    .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                    .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
            
            // Tính toán theo phương pháp lãi suất giảm dần (Equal Principal)
            BigDecimal monthlyPrincipal = loan.getAmount()
                    .divide(BigDecimal.valueOf(loan.getTermMonths()), 2, RoundingMode.HALF_UP);
            LocalDate startDueDate = LocalDate.now().plusMonths(1);
            
            // Điều chỉnh principal kỳ cuối để tránh sai số do làm tròn
            BigDecimal totalPrincipal = BigDecimal.ZERO;
            BigDecimal remainingAmount = loan.getAmount();

            for (int i = 0; i < loan.getTermMonths(); i++) {
                BigDecimal currentPrincipal;
                if (i == loan.getTermMonths() - 1) {
                    // Kỳ cuối: lấy phần còn lại để đảm bảo tổng = loan amount
                    currentPrincipal = remainingAmount;
                } else {
                    currentPrincipal = monthlyPrincipal;
                }
                
                // Tính lãi trên dư nợ đầu kỳ
                BigDecimal interest = remainingAmount
                        .multiply(monthlyInterestRate)
                        .setScale(2, RoundingMode.HALF_UP);
                
                Repayment repayment = new Repayment();
                repayment.setLoan(loan);
                repayment.setDueDate(startDueDate.plusMonths(i));
                repayment.setPrincipal(currentPrincipal);
                repayment.setInterest(interest);
                repayment.setPaidAmount(BigDecimal.ZERO);
                repayment.setStatus(RepaymentStatus.UNPAID);
                
                repaymentRepository.save(repayment);
                repayments.add(repayment);
                
                // Cập nhật dư nợ cho kỳ tiếp theo
                remainingAmount = remainingAmount.subtract(currentPrincipal);
                totalPrincipal = totalPrincipal.add(currentPrincipal);
                
                log.debug("PERIOD_{} - principal: {}, interest: {}, remaining: {}", 
                    i + 1, currentPrincipal, interest, remainingAmount);
            }
            
            // Kiểm tra tổng principal phải bằng loan amount
            if (totalPrincipal.compareTo(loan.getAmount()) != 0) {
                log.warn("PRINCIPAL_TOTAL_MISMATCH - expected: {}, actual: {}", loan.getAmount(), totalPrincipal);
            }
            
            log.info("GENERATE_REPAYMENT_SCHEDULE_SUCCESS - loanId: {}, totalPeriods: {}, totalPrincipal: {}", 
                loan.getLoanId(), repayments.size(), totalPrincipal);
            return repayments;
        } catch (Exception e) {
            log.error("GENERATE_REPAYMENT_SCHEDULE_ERROR - loanId: {}, error: {}", loan.getLoanId(), e.getMessage(), e);
            throw e;
        }
    }
    @Override
    public List<Repayment> getOverdueRepayments(Long loanId) {
        return repaymentRepository.findOverdueByLoanId(loanId, RepaymentStatus.LATE);
    }

    @Override
    public List<Repayment> getAllOverdueRepayments() {
        log.info("GET_ALL_OVERDUE_REPAYMENTS_START");
        try {
            LocalDate today = LocalDate.now();
            List<Repayment> repayments = repaymentRepository.findAllOverdueRepayments(today);
            log.info("GET_ALL_OVERDUE_REPAYMENTS_SUCCESS - count: {}", repayments.size());
            return repayments;
        } catch (Exception e) {
            log.error("GET_ALL_OVERDUE_REPAYMENTS_ERROR - error: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<Repayment> getUpcomingRepayments(Long loanId) {
        return repaymentRepository.findUpcomingByLoanId(loanId, LocalDate.now(), LocalDate.now().plusDays(7));
    }
    @Override
    public List<Repayment> getRepaymentsByLoanId(Long loanId) {
        log.info("GET_REPAYMENTS_BY_LOAN_START - loanId: {}", loanId);
        try {
            List<Repayment> list = repaymentRepository.findAllByLoan_LoanIdOrderByDueDateAsc(loanId);
            log.info("GET_REPAYMENTS_BY_LOAN_SUCCESS - loanId: {}, count: {}", loanId, list.size());
            return list;
        } catch (Exception e) {
            log.error("GET_REPAYMENTS_BY_LOAN_ERROR - loanId: {}, error: {}", loanId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Optional<Repayment> getRepaymentById(Long repaymentId) {
        // giữ nguyên logging đã có
        log.info("GET_REPAYMENT_BY_ID_START - repaymentId: {}", repaymentId);
        try {
            Optional<Repayment> repayment = repaymentRepository.findById(repaymentId);
            log.info("GET_REPAYMENT_BY_ID_SUCCESS - repaymentId: {}, found: {}", repaymentId, repayment.isPresent());
            return repayment;
        } catch (Exception e) {
            log.error("GET_REPAYMENT_BY_ID_ERROR - repaymentId: {}, error: {}", repaymentId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<Repayment> getHistoryRepayment(Long customerId) {
        log.info("GET_REPAYMENT_HISTORY_START - customerId: {}", customerId);
        try {
            List<Repayment> list = repaymentRepository.findPaidOrPartialByLoanId(customerId);
            log.info("GET_REPAYMENT_HISTORY_SUCCESS - customerId: {}, count: {}", customerId, list.size());
            return list;
        } catch (Exception e) {
            log.error("GET_REPAYMENT_HISTORY_ERROR - customerId: {}, error: {}", customerId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Repayment updateRepaymentStatus(Long repaymentId, RepaymentStatus status) {
        log.info("UPDATE_REPAYMENT_STATUS_START - repaymentId: {}, status: {}", repaymentId, status);
        try {
            Repayment repayment = repaymentRepository.findById(repaymentId)
                    .orElseThrow(() -> new EntityNotFoundException("Repayment not found: " + repaymentId));
            repayment.setStatus(status);
            Repayment saved = repaymentRepository.save(repayment);
            log.info("UPDATE_REPAYMENT_STATUS_SUCCESS - repaymentId: {}, newStatus: {}", repaymentId, status);
            return saved;
        } catch (EntityNotFoundException e) {
            log.warn("UPDATE_REPAYMENT_STATUS_NOT_FOUND - repaymentId: {}, error: {}", repaymentId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("UPDATE_REPAYMENT_STATUS_ERROR - repaymentId: {}, error: {}", repaymentId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Repayment updateRepayment(Repayment repayment) {
        log.info("UPDATE_REPAYMENT_START - repaymentId: {}", repayment.getRepaymentId());
        try {
            Repayment saved = repaymentRepository.save(repayment);
            log.info("UPDATE_REPAYMENT_SUCCESS - repaymentId: {}", repayment.getRepaymentId());
            return saved;
        } catch (Exception e) {
            log.error("UPDATE_REPAYMENT_ERROR - repaymentId: {}, error: {}", repayment.getRepaymentId(), e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Repayment makeRepayment(Long repaymentId, BigDecimal amount) {
        log.info("MAKE_REPAYMENT_START - repaymentId: {}, amount: {}", repaymentId, amount);
        try {
            Repayment repayment = repaymentRepository.findById(repaymentId)
                    .orElseThrow(() -> new EntityNotFoundException("Repayment not found: " + repaymentId));
            BigDecimal newPaid = repayment.getPaidAmount().add(amount);
            repayment.setPaidAmount(newPaid);

            BigDecimal totalDue = repayment.getPrincipal().add(repayment.getInterest());
            log.debug("MAKE_REPAYMENT_CALC - repaymentId: {}, totalDue: {}, newPaid: {}", repaymentId, totalDue, newPaid);
            Repayment saved = new Repayment();
            if (newPaid.compareTo(totalDue) >= 0) {
                repayment.setStatus(RepaymentStatus.PAID);
                saved = repaymentRepository.save(repayment);
                // Kiểm tra xem khoản vay có thể đóng không

            } else if (newPaid.compareTo(BigDecimal.ZERO) > 0) {
                repayment.setStatus(RepaymentStatus.PARTIAL);
                saved = repaymentRepository.save(repayment);
            }



            log.info("MAKE_REPAYMENT_SUCCESS - repaymentId: {}, status: {}", repaymentId, saved.getStatus());
            return saved;
        } catch (EntityNotFoundException e) {
            log.warn("MAKE_REPAYMENT_NOT_FOUND - repaymentId: {}", repaymentId);
            throw e;
        } catch (Exception e) {
            log.error("MAKE_REPAYMENT_ERROR - repaymentId: {}, error: {}", repaymentId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void deleteRepaymentsByLoanId(Long loanId) {
        log.info("DELETE_REPAYMENTS_BY_LOAN_START - loanId: {}", loanId);
        try {
            List<Repayment> list = getRepaymentsByLoanId(loanId);
            repaymentRepository.deleteAll(list);
            log.info("DELETE_REPAYMENTS_BY_LOAN_SUCCESS - loanId: {}, deletedCount: {}", loanId, list.size());
        } catch (Exception e) {
            log.error("DELETE_REPAYMENTS_BY_LOAN_ERROR - loanId: {}, error: {}", loanId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Repayment getCurrentRepayment(Long customerId) {
        log.info("GET_CURRENT_REPAYMENT_START - customerId: {}", customerId);
        try {
            Long loanId = loanRepository.findAllByStatusIsAndCustomerId(LoanStatus.APPROVED, customerId)
                    .get(0).getLoanId();
            Repayment r = repaymentRepository.findNextRepaymentNative(loanId);
            log.info("GET_CURRENT_REPAYMENT_SUCCESS - customerId: {}, repaymentId: {}", customerId, r.getRepaymentId());
            return r;
        } catch (Exception e) {
            log.error("GET_CURRENT_REPAYMENT_ERROR - customerId: {}, error: {}", customerId, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Repayment getCurrentRepaymentbyLoanId(Long loanId) {
        log.info("GET_CURRENT_REPAYMENT_BY_LOAN_START - loanId: {}", loanId);
        try {
            Repayment r = repaymentRepository.findNextRepaymentNative(loanId);
            log.info("GET_CURRENT_REPAYMENT_BY_LOAN_SUCCESS - loanId: {}, repaymentId: {}", loanId, r != null ? r.getRepaymentId() : null);
            return r;
        } catch (Exception e) {
            log.error("GET_CURRENT_REPAYMENT_BY_LOAN_ERROR - loanId: {}, error: {}", loanId, e.getMessage(), e);
            throw e;
        }
    }

//    @Override
//    public List<Repayment> updateRepaymentSchedule(Loan loan, int startPeriodIndex, BigDecimal remainingPrincipal) {
//        log.info("UPDATE_REPAYMENT_SCHEDULE_START - loanId: {}, startIndex: {}", loan.getLoanId(), startPeriodIndex);
//        try {
//            List<Repayment> updated = new ArrayList<>();
//            BigDecimal monthlyInterestRate = loan.getInterestRate()
//                    .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
//                    .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
//            int remainingTerm = loan.getTermMonths() - startPeriodIndex;
//            BigDecimal updatedPrincipal = remainingPrincipal
//                    .divide(BigDecimal.valueOf(remainingTerm), 2, RoundingMode.HALF_UP);
//            LocalDate startDue = LocalDate.now().plusMonths(1);
//
//            for (int i = 0; i < remainingTerm; i++) {
//                Repayment r = calculateRepayment(loan, startPeriodIndex + i, updatedPrincipal, monthlyInterestRate, startDue);
//                repaymentRepository.save(r);
//                updated.add(r);
//            }
//            log.info("UPDATE_REPAYMENT_SCHEDULE_SUCCESS - loanId: {}, newCount: {}", loan.getLoanId(), updated.size());
//            return updated;
//        } catch (Exception e) {
//            log.error("UPDATE_REPAYMENT_SCHEDULE_ERROR - loanId: {}, error: {}", loan.getLoanId(), e.getMessage(), e);
//            throw e;
//        }
//    }

    @Override
    public Boolean checkLastMonthRepayment(Repayment repayment) {
        log.info("CHECK_LAST_MONTH_REPAYMENT_START - repaymentId: {}", repayment.getRepaymentId());
        try {
            List<Repayment> repayments = getRepaymentsByLoanId(repayment.getLoan().getLoanId());
            
            // Sắp xếp theo dueDate để tìm kỳ cuối cùng
            repayments.sort((r1, r2) -> r1.getDueDate().compareTo(r2.getDueDate()));
            
            // Kiểm tra xem repayment hiện tại có phải là kỳ cuối cùng không
            Repayment lastRepayment = repayments.get(repayments.size() - 1);
            boolean isLast = repayment.getRepaymentId().equals(lastRepayment.getRepaymentId());
            
            log.info("CHECK_LAST_MONTH_REPAYMENT_SUCCESS - repaymentId: {}, isLast: {}, totalPeriods: {}", 
                repayment.getRepaymentId(), isLast, repayments.size());
            return isLast;
        } catch (Exception e) {
            log.error("CHECK_LAST_MONTH_REPAYMENT_ERROR - repaymentId: {}, error: {}", repayment.getRepaymentId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Kiểm tra xem khoản vay có thể đóng không
     * Điều kiện: Tất cả các kỳ trả nợ đã được thanh toán đầy đủ
     */
    @Override
    public Boolean shouldCloseLoan(Long loanId) {
        log.info("SHOULD_CLOSE_LOAN_START - loanId: {}", loanId);
        try {
            boolean hasUnpaid = repaymentRepository.existsUnpaidRepaymentByLoanId(loanId);
            if (hasUnpaid) {
                log.info("SHOULD_CLOSE_LOAN_FALSE - loanId: {}, còn kỳ chưa trả đủ", loanId);
                return false;
            }
            log.info("SHOULD_CLOSE_LOAN_TRUE - loanId: {}, all repayments paid", loanId);
            return true;
        } catch (Exception e) {
            log.error("SHOULD_CLOSE_LOAN_ERROR - loanId: {}, error: {}", loanId, e.getMessage(), e);
            return false;
        }
    }

    private Repayment calculateRepayment(Loan loan, int periodIndex, BigDecimal monthlyPrincipal, BigDecimal monthlyInterestRate, LocalDate startDueDate) {
        log.info("CALCULATE_REPAYMENT_START - loanId: {}, periodIndex: {}",
                loan.getLoanId(), periodIndex);
        try {
            BigDecimal remainingPrincipal = loan.getAmount()
                    .subtract(monthlyPrincipal.multiply(BigDecimal.valueOf(periodIndex)));
            BigDecimal interest = remainingPrincipal
                    .multiply(monthlyInterestRate)
                    .setScale(2, RoundingMode.HALF_UP);

            Repayment repayment = new Repayment();
            repayment.setLoan(loan);
            repayment.setDueDate(startDueDate.plusMonths(periodIndex));
            repayment.setPrincipal(monthlyPrincipal);
            repayment.setInterest(interest);
            repayment.setPaidAmount(BigDecimal.ZERO);
            repayment.setStatus(RepaymentStatus.UNPAID);

            log.debug("CALCULATE_REPAYMENT_RESULT - periodIndex: {}, dueDate: {}, principal: {}, interest: {}",
                    periodIndex,
                    repayment.getDueDate(),
                    repayment.getPrincipal(),
                    repayment.getInterest());
            log.info("CALCULATE_REPAYMENT_SUCCESS - loanId: {}, periodIndex: {}",
                    loan.getLoanId(), periodIndex);
            return repayment;
        } catch (Exception e) {
            log.error("CALCULATE_REPAYMENT_ERROR - loanId: {}, periodIndex: {}, error: {}",
                    loan.getLoanId(), periodIndex, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public java.math.BigDecimal getTotalCollectedSystem() {
        List<Repayment> all = repaymentRepository.findAll();
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        for (Repayment r : all) {
            if (r.getStatus() == RepaymentStatus.PAID || r.getStatus() == RepaymentStatus.PARTIAL) {
                total = total.add(r.getPaidAmount());
            }
        }
        return total;
    }

    @Override
    public BigDecimal getTotalProfitSystem() {
        // 1 query tính tổng profit
        return repaymentRepository.sumProfit();
    }

    @Override
    public Map<String, Long> getRepaymentStats() {
        List<Object[]> rows = repaymentRepository.countByStatus();
        // khởi map với 0 cho mọi trạng thái
        Map<String, Long> stats = new HashMap<>();
        stats.put("paid",    0L);
        stats.put("partial", 0L);
        stats.put("unpaid",  0L);
        stats.put("late",    0L);

        for (Object[] row : rows) {
            RepaymentStatus status = (RepaymentStatus) row[0];
            Long count             = (Long) row[1];
            stats.put(status.name().toLowerCase(), count);
        }
        return stats;
    }

    @Override
    public BigDecimal getOutstandingDebtByLoanId(Long loanId) {
        return repaymentRepository.getOutstandingDebtByLoanId(loanId);
    }


}
