package com.example.loan_service.scheduler;

import com.example.common_service.dto.*;
import com.example.common_service.dto.request.CommonDisburseRequest;
import com.example.common_service.dto.request.LoanRequestDTO;
import com.example.common_service.dto.request.AutoDeductRepaymentRequest;
import com.example.common_service.services.account.AccountQueryService;
import com.example.common_service.services.customer.CustomerQueryService;
import com.example.common_service.services.transactions.CommonTransactionService;
import com.example.common_service.services.account.AccountDubboService;
import com.example.loan_service.entity.Loan;
import com.example.loan_service.entity.Repayment;
import com.example.loan_service.mapper.LoanMapper;
import com.example.loan_service.mapper.CoreAccountMapper;
import com.example.loan_service.models.RepaymentStatus;
import com.example.common_service.constant.LoanStatus;
import com.example.loan_service.service.CoreBankingClient;
import com.example.loan_service.service.LoanService;
import com.example.loan_service.service.RepaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RepaymentCheckScheduler {

    private final LoanService loanService;
    private final RepaymentService repaymentService;
    private final LoanMapper loanMapper;
    private final CoreBankingClient coreBankingClient;
    private final StreamBridge streamBridge;
    private final com.example.loan_service.repository.RepaymentRepository repaymentRepository;
    @DubboReference private final CustomerQueryService customerQueryService;
    @DubboReference private final CommonTransactionService commonTransactionService;
    @DubboReference private final AccountDubboService accountDubboService;
    @DubboReference private final AccountQueryService accountQueryService;
    @Scheduled(fixedRateString = "${repayment.scheduler.fix-rate:5000000}")
    public void checkAndHandleLateRepayments() {
        log.info("CHECK_LATE_REPAYMENTS_START");
        for (Loan loan : loanService.getLoansApprove()) {
            List<Repayment> overdue = repaymentService.getOverdueRepayments(loan.getLoanId());
            if (overdue.isEmpty()) continue;

            Repayment r = overdue.get(0);
            log.info("MARK_REPAYMENT_LATE_START - repaymentId: {}", r.getRepaymentId());

            // Kiểm tra xem tháng trước có trễ hạn không
            boolean previousMonthLate = checkPreviousMonthLate(loan.getLoanId(), r.getDueDate());
            
            if (previousMonthLate) {
                // Nếu tháng trước cũng trễ hạn, đóng khoản vay và thu hồi
                log.info("CLOSE_LOAN_DUE_TO_CONSECUTIVE_LATE - loanId: {}", loan.getLoanId());
                closeLoanAndRecover(loan);
                continue;
            }

            // đánh dấu trễ
            r.setStatus(RepaymentStatus.LATE);
            repaymentService.updateRepayment(r);
            log.info("MARK_REPAYMENT_LATE_SUCCESS - repaymentId: {}", r.getRepaymentId());

            // Tính số tiền chưa trả (gốc + lãi)
            BigDecimal unpaid = r.getPrincipal().add(r.getInterest()).subtract(r.getPaidAmount());
            if (unpaid.compareTo(BigDecimal.ZERO) < 0) unpaid = BigDecimal.ZERO;
            // Phạt 1.5% trên tổng số tiền chưa trả
            BigDecimal penalty = unpaid.multiply(BigDecimal.valueOf(0.015)).setScale(2, BigDecimal.ROUND_HALF_UP);

            boolean isLast = repaymentService.checkLastMonthRepayment(r);
            if (isLast) {
                log.info("CREATE_PENALTY_REPAYMENT_START - repaymentId: {}", r.getRepaymentId());
                Repayment p = new Repayment();
                p.setLoan(r.getLoan());
                p.setDueDate(r.getDueDate().plusMonths(1));
                p.setPrincipal(unpaid); // phần gốc chưa trả
                p.setInterest(penalty); // chỉ phạt, không cộng lãi cũ nữa
                p.setPaidAmount(BigDecimal.ZERO);
                p.setStatus(RepaymentStatus.UNPAID);
                repaymentService.updateRepayment(p);
                // Đồng bộ outstandingDebt thực tế lên corebanking
                BigDecimal outstandingDebt = repaymentRepository.getOutstandingDebtByLoanId(loan.getLoanId());
                CoreAccountRequest coreAccountRequest = CoreAccountMapper.INSTANCE.fromLoan(loan);
                coreAccountRequest.setBalance(outstandingDebt);
                coreBankingClient.updateAccount(coreAccountRequest);
                log.info("CREATE_PENALTY_REPAYMENT_SUCCESS - newDueDate: {}",
                        p.getDueDate().format(DateTimeFormatter.ISO_DATE));
            } else {
                log.info("ROLL_FORWARD_PENALTY_START - repaymentId: {}", r.getRepaymentId());
                Repayment current = repaymentService.getCurrentRepaymentbyLoanId(loan.getLoanId());
                if (current != null) {
                    // Cộng dồn phần chưa trả và phạt vào kỳ tiếp theo
                    current.setPrincipal(current.getPrincipal().add(unpaid));
                    current.setInterest(current.getInterest().add(penalty));
                    repaymentService.updateRepayment(current);
                    // Đồng bộ outstandingDebt thực tế lên corebanking
                    BigDecimal outstandingDebt = repaymentRepository.getOutstandingDebtByLoanId(loan.getLoanId());
                    CoreAccountRequest coreAccountRequest = CoreAccountMapper.INSTANCE.fromLoan(loan);
                    coreAccountRequest.setBalance(outstandingDebt);
                    coreBankingClient.updateAccount(coreAccountRequest);
                    log.info("ROLL_FORWARD_PENALTY_SUCCESS - currentRepaymentId: {}", current.getRepaymentId());
                } else {
                    log.warn("ROLL_FORWARD_PENALTY_SKIPPED - no current repayment for loanId: {}",
                            loan.getLoanId());
                }
            }

            // gửi thông báo trễ
            CustomerResponseDTO cust = customerQueryService.getCustomerById(loan.getCustomerId());
            String body = String.format(
                    "Kính chào %s,%n%n" +
                            "Khoản vay ID: %s (TK: %s) quá hạn từ %s.%n" +
                            "Số tiền kỳ này: %s VND.%n%n" +
                            "Vui lòng thanh toán để tránh ảnh hưởng lịch sử tín dụng.%n%n" +
                            "Trân trọng, Ngân hàng",
                    cust.getFullName(),
                    loan.getLoanId(),
                    loan.getDisbursementAccountNumber(),
                    r.getDueDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    r.getPrincipal().add(r.getInterest())
            );
            MailMessageDTO mail = MailMessageDTO.builder()
                    .subject("THÔNG BÁO TRẢ TRễ VAY")
                    .recipient(cust.getEmail())
                    .recipientName(cust.getFullName())
                    .body(body)
                    .build();
            streamBridge.send("mail-out-0", mail);
            log.info("LATE_REPAYMENT_NOTIFICATION_SENT - loanId: {}", loan.getLoanId());
        }
        log.info("CHECK_LATE_REPAYMENTS_SUCCESS");
    }

    /**
     * Kiểm tra xem tháng trước có trễ hạn không
     */
    private boolean checkPreviousMonthLate(Long loanId, LocalDate currentDueDate) {
        try {
            LocalDate previousMonthDueDate = currentDueDate.minusMonths(1);
            List<Repayment> repayments = repaymentService.getRepaymentsByLoanId(loanId);
            
            for (Repayment repayment : repayments) {
                if (repayment.getDueDate().equals(previousMonthDueDate)) {
                    // Kiểm tra status UNPAID hoặc PARTIAL
                    if (repayment.getStatus() == RepaymentStatus.UNPAID || 
                        repayment.getStatus() == RepaymentStatus.PARTIAL) {
                        log.info("PREVIOUS_MONTH_LATE_FOUND - loanId: {}, dueDate: {}, status: {}", 
                            loanId, previousMonthDueDate, repayment.getStatus());
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            log.error("CHECK_PREVIOUS_MONTH_LATE_ERROR - loanId: {}, error: {}", loanId, e.getMessage());
            return false;
        }
    }

    /**
     * Đóng khoản vay và thu hồi tiền từ loan account
     */
    private void closeLoanAndRecover(Loan loan) {
        try {
            log.info("CLOSE_LOAN_AND_RECOVER_START - loanId: {}", loan.getLoanId());
            
            // 1. Kiểm tra số dư loan account
            BigDecimal loanBalance = getLoanAccountBalance(loan.getDisbursementAccountNumber());
            log.info("LOAN_ACCOUNT_BALANCE - account: {}, balance: {}", loan.getDisbursementAccountNumber(), loanBalance);
            
            // 2. Đóng khoản vay
            loanService.closedLoan(loan.getLoanId());
            
            // 3. Chỉ thu hồi nếu có tài sản
            if (loanBalance.compareTo(BigDecimal.ZERO) > 0) {
                log.info("RECOVER_LOAN_AMOUNT - amount: {}", loanBalance);
                CommonDisburseRequest recoverRequest = new CommonDisburseRequest();
                recoverRequest.setToAccountNumber(loan.getDisbursementAccountNumber()); // Master account của ngân hàng
                recoverRequest.setAmount(loanBalance); // Thu hồi số dư thực tế
                recoverRequest.setCurrency("VND");
                recoverRequest.setDescription("Thu hồi khoản vay do vi phạm điều khoản trả nợ");
                
                CommonTransactionDTO tx = commonTransactionService.loanRecovery(recoverRequest);
                log.info("RECOVER_LOAN_TRANSACTION - status: {}, ref: {}", tx.getStatus(), tx.getReferenceCode());
                
                if (!"COMPLETED".equalsIgnoreCase(tx.getStatus())) {
                    log.error("RECOVER_LOAN_FAILED - reason: {}", tx.getFailedReason());
                    throw new RuntimeException("Thu hồi khoản vay thất bại: " + tx.getFailedReason());
                }
            } else {
                log.info("SKIP_RECOVERY - loan account balance is zero");
            }
            
            // 3. Cập nhật account service
            LoanRequestDTO updateDto = new LoanRequestDTO();
            updateDto.setLoanId(loan.getLoanId());
            updateDto.setAmount(BigDecimal.ZERO); // Số dư = 0 sau khi thu hồi
            updateDto.setStatus(LoanStatus.CLOSED);
            accountDubboService.updateAccountFromLoan(updateDto);
            
            // 4. Cập nhật core banking
            CoreAccountRequest coreAccountRequest = CoreAccountMapper.INSTANCE.fromLoan(loan);
            coreAccountRequest.setBalance(BigDecimal.ZERO);
            coreBankingClient.updateAccount(coreAccountRequest);
            
            // 5. Gửi thông báo đóng khoản vay
            CustomerResponseDTO cust = customerQueryService.getCustomerById(loan.getCustomerId());
            String body = String.format(
                    "Kính chào %s,%n%n" +
                            "Khoản vay ID: %s đã bị đóng do vi phạm điều khoản trả nợ.%n" +
                            "Số tiền đã thu hồi: %s VND.%n%n" +
                            "Vui lòng liên hệ ngân hàng để biết thêm chi tiết.%n%n" +
                            "Trân trọng, Ngân hàng",
                    cust.getFullName(),
                    loan.getLoanId(),
                    loan.getAmount()
            );
            MailMessageDTO mail = MailMessageDTO.builder()
                    .subject("THÔNG BÁO ĐÓNG KHOẢN VAY")
                    .recipient(cust.getEmail())
                    .recipientName(cust.getFullName())
                    .body(body)
                    .build();
            streamBridge.send("mail-out-0", mail);
            
            log.info("CLOSE_LOAN_AND_RECOVER_SUCCESS - loanId: {}", loan.getLoanId());
        } catch (Exception e) {
            log.error("CLOSE_LOAN_AND_RECOVER_ERROR - loanId: {}, error: {}", loan.getLoanId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Lấy số dư của loan account
     */
    private BigDecimal getLoanAccountBalance(String accountNumber) {
        try {
            // Gọi account service để lấy số dư
            AccountDTO account = accountQueryService.getAccountByAccountNumber(accountNumber);
            return account.getBalance();
        } catch (Exception e) {
            log.error("GET_LOAN_ACCOUNT_BALANCE_ERROR - account: {}, error: {}", accountNumber, e.getMessage());
            return BigDecimal.ZERO; // Trả về 0 nếu có lỗi
        }
    }

    /**
     * Lấy số dư của repayment account
     */
    private BigDecimal getRepaymentAccountBalance(String accountNumber) {
        try {
            // Gọi account service để lấy số dư
            AccountDTO account = accountQueryService.getAccountByAccountNumber(accountNumber);
            return account.getBalance();
        } catch (Exception e) {
            log.error("GET_REPAYMENT_ACCOUNT_BALANCE_ERROR - account: {}, error: {}", accountNumber, e.getMessage());
            return BigDecimal.ZERO; // Trả về 0 nếu có lỗi
        }
    }

    /**
     * Lấy danh sách kỳ trả nợ đến hạn hôm nay
     */
    private List<Repayment> getRepaymentsDueToday(Long loanId) {
        try {
            LocalDate today = LocalDate.now();
            List<Repayment> allRepayments = repaymentService.getRepaymentsByLoanId(loanId);
            return allRepayments.stream()
                    .filter(r -> r.getDueDate().equals(today))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("GET_REPAYMENTS_DUE_TODAY_ERROR - loanId: {}, error: {}", loanId, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Thực hiện tự động trừ tiền
     */
    private void performAutoDeduct(Loan loan, Repayment repayment, BigDecimal amount) {
        try {
            log.info("PERFORM_AUTO_DEDUCT_START - loanId: {}, repaymentId: {}, amount: {}", 
                loan.getLoanId(), repayment.getRepaymentId(), amount);
            
            AutoDeductRepaymentRequest autoDeductRequest = AutoDeductRepaymentRequest.builder()
                    .fromAccountNumber(loan.getRepaymentAccountNumber())
                    .toAccountNumber("MASTER_ACCOUNT")
                    .amount(amount)
                    .currency("VND")
                    .description("Tự động trừ tiền định kỳ - Kỳ " + repayment.getDueDate().format(DateTimeFormatter.ofPattern("MM/yyyy")))
                    .loanId(loan.getLoanId())
                    .repaymentId(repayment.getRepaymentId())
                    .build();
            
            CommonTransactionDTO tx = commonTransactionService.autoDeductRepayment(autoDeductRequest);
            log.info("AUTO_DEDUCT_TRANSACTION - status: {}, ref: {}", tx.getStatus(), tx.getReferenceCode());
            
            if ("COMPLETED".equalsIgnoreCase(tx.getStatus())) {
                // Cập nhật trạng thái repayment
                repayment.setPaidAmount(amount);
                repayment.setStatus(RepaymentStatus.PAID);
                repaymentService.updateRepayment(repayment);
                
                // Kiểm tra xem có phải kỳ cuối không
                if (shouldCloseLoan(repayment.getLoan().getLoanId())) {
                    log.info("CLOSE_LOAN_AFTER_AUTO_DEDUCT - loanId: {}", loan.getLoanId());
                    loanService.closedLoan(loan.getLoanId());
                }
                
                log.info("PERFORM_AUTO_DEDUCT_SUCCESS - loanId: {}, repaymentId: {}", 
                    loan.getLoanId(), repayment.getRepaymentId());
            } else {
                log.error("AUTO_DEDUCT_FAILED - reason: {}", tx.getFailedReason());
                // Đánh dấu trễ hạn nếu tự động trừ thất bại
                repayment.setStatus(RepaymentStatus.LATE);
                repaymentService.updateRepayment(repayment);
            }
        } catch (Exception e) {
            log.error("PERFORM_AUTO_DEDUCT_ERROR - loanId: {}, repaymentId: {}, error: {}", 
                loan.getLoanId(), repayment.getRepaymentId(), e.getMessage(), e);
            // Đánh dấu trễ hạn nếu có lỗi
            repayment.setStatus(RepaymentStatus.LATE);
            repaymentService.updateRepayment(repayment);
        }
    }

    @Scheduled(cron = "${repayment.scheduler.auto-deduct-cron:0 0 8 * * *}")
    public void autoDeductRepayments() {
        log.info("AUTO_DEDUCT_REPAYMENTS_START");
        for (Loan loan : loanService.getLoansApprove()) {
            try {
                // Lấy kỳ trả nợ đến hạn hôm nay
                List<Repayment> dueToday = getRepaymentsDueToday(loan.getLoanId());
                
                for (Repayment repayment : dueToday) {
                    if (repayment.getStatus() == RepaymentStatus.UNPAID) {
                        log.info("AUTO_DEDUCT_REPAYMENT_START - loanId: {}, repaymentId: {}", 
                            loan.getLoanId(), repayment.getRepaymentId());
                        
                        // Kiểm tra số dư repayment account
                        BigDecimal repaymentBalance = getRepaymentAccountBalance(loan.getRepaymentAccountNumber());
                        BigDecimal requiredAmount = repayment.getPrincipal().add(repayment.getInterest());
                        
                        log.info("REPAYMENT_ACCOUNT_BALANCE - account: {}, balance: {}, required: {}", 
                            loan.getRepaymentAccountNumber(), repaymentBalance, requiredAmount);
                        
                        if (repaymentBalance.compareTo(requiredAmount) >= 0) {
                            // Có đủ tiền, thực hiện tự động trừ
                            performAutoDeduct(loan, repayment, requiredAmount);
                        } else {
                            // Không đủ tiền, đánh dấu trễ hạn
                            log.warn("INSUFFICIENT_BALANCE - account: {}, balance: {}, required: {}", 
                                loan.getRepaymentAccountNumber(), repaymentBalance, requiredAmount);
                            repayment.setStatus(RepaymentStatus.LATE);
                            repaymentService.updateRepayment(repayment);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("AUTO_DEDUCT_REPAYMENT_ERROR - loanId: {}, error: {}", loan.getLoanId(), e.getMessage(), e);
            }
        }
        log.info("AUTO_DEDUCT_REPAYMENTS_SUCCESS");
    }

    @Scheduled(cron = "${repayment.scheduler.remind-cron:0 0 9 * * *}")
    public void remindUpcomingRepayments() {
        log.info("REMIND_UPCOMING_REPAYMENTS_START");
        for (Loan loan : loanService.getLoansApprove()) {
            List<Repayment> upcoming = repaymentService.getUpcomingRepayments(loan.getLoanId());
            if (upcoming.isEmpty()) continue;

            Repayment next = upcoming.get(0);
            long daysToDue = ChronoUnit.DAYS.between(LocalDate.now(), next.getDueDate());
            if (daysToDue < 1 || daysToDue > 3) continue; 
            CustomerResponseDTO cust = customerQueryService.getCustomerById(loan.getCustomerId());
            String body = String.format(
                    "Kính chào %s,%n%n" +
                            "Bạn còn %d ngày đến kỳ thanh toán khoản vay ID: %s với tổng số tiền %s VND.%n%n" +
                            "Vui lòng chuẩn bị để tránh phát sinh phí trễ.%n%n" +
                            "Trân trọng, Ngân hàng",
                    cust.getFullName(),
                    daysToDue,
                    loan.getLoanId(),
                    next.getPrincipal().add(next.getInterest())
            );
            MailMessageDTO mail = MailMessageDTO.builder()
                    .subject("NHẮC NỢ ĐỊNH KỲ")
                    .recipient(cust.getEmail())
                    .recipientName(cust.getFullName())
                    .body(body)
                    .build();
            streamBridge.send("mail-out-0", mail);
            log.info("REMIND_UPCOMING_REPAYMENT_NOTIFICATION_SENT - repaymentId: {}", next.getRepaymentId());
        }
        log.info("REMIND_UPCOMING_REPAYMENTS_SUCCESS");
    }

    /**
     * Kiểm tra xem đã trả hết nợ cho khoản vay chưa (không còn kỳ unpaid hoặc partial)
     */
    private boolean shouldCloseLoan(Long loanId) {
        // Dùng JPA để kiểm tra còn kỳ chưa trả đủ
        return !repaymentRepository.existsUnpaidRepaymentByLoanId(loanId);
    }
}
