package com.example.loan_service.scheduler;

import com.example.common_service.constant.AccountStatus;
import com.example.common_service.dto.*;
import com.example.common_service.dto.request.CommonDisburseRequest;
import com.example.common_service.dto.request.LoanRequestDTO;
import com.example.common_service.dto.request.AutoDeductRepaymentRequest;
import com.example.common_service.services.account.AccountQueryService;
import com.example.common_service.services.customer.CustomerQueryService;
import com.example.common_service.services.transactions.CommonTransactionService;
import com.example.common_service.services.account.AccountDubboService;
import com.example.loan_service.entity.Loan;
import com.example.common_service.constant.LoanType;
import com.example.loan_service.entity.Repayment;
import com.example.loan_service.mapper.LoanMapper;
import com.example.loan_service.mapper.CoreAccountMapper;
import com.example.loan_service.models.RepaymentStatus;
import com.example.common_service.constant.LoanStatus;
import com.example.loan_service.service.CoreBankingClient;
import com.example.loan_service.service.LoanService;
import com.example.loan_service.service.RepaymentService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
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

    /**
     * Xử lý các kỳ trả nợ đã quá hạn (từ ngày hôm qua trở về trước)
     * Không xử lý các kỳ trả nợ đến hạn hôm nay để cho phép khách hàng chủ động thanh toán
     */

    @Transactional
//    @Scheduled(cron = "0 0 3 * * ?")
    @Scheduled(fixedRate = 60 * 60 * 1000)
    public void processRepayments() {
        log.info("PROCESS_OVERDUE_REPAYMENTS_START");
        // Lấy trực tiếp tất cả các kỳ trả nợ đã quá hạn
        List<Repayment> overdueRepayments = repaymentService.getAllOverdueRepayments();
        if (overdueRepayments.isEmpty()) {
            log.info("PROCESS_OVERDUE_REPAYMENTS_NO_OVERDUE");
            return;
        }
        for (Repayment repayment : overdueRepayments) {
            try {
                Loan loan = repayment.getLoan();
                BigDecimal requiredAmount = repayment.getPrincipal().add(repayment.getInterest());
                log.info("PROCESS_OVERDUE_REPAYMENT_START - loanId: {}, repaymentId: {}, dueDate: {}", 
                    loan.getLoanId(), repayment.getRepaymentId(), repayment.getDueDate());
                // Kiểm tra số dư repayment account
                BigDecimal repaymentBalance = getRepaymentAccountBalance(loan.getRepaymentAccountNumber());
                log.info("REPAYMENT_ACCOUNT_BALANCE - account: {}, balance: {}, required: {}", 
                    loan.getRepaymentAccountNumber(), repaymentBalance, requiredAmount);
                if (repaymentBalance.compareTo(requiredAmount) >= 0) {
                    // Có đủ tiền, thực hiện tự động trừ
                    performAutoDeduct(loan, repayment, requiredAmount);
                } else {
                    Integer previousMonthLate = repaymentService.checkPreviousMonthLate(repayment.getRepaymentId(),loan.getLoanId());
                    if (previousMonthLate >= 3) {
                        // Nếu tháng trước cũng trễ hạn, đóng khoản vay và thu hồi
                        log.info("CLOSE_LOAN_DUE_TO_CONSECUTIVE_LATE - loanId: {}", loan.getLoanId());
                        closeLoanAndRecover(loan);
                        repayment.setStatus(RepaymentStatus.LATE);
                        repaymentService.updateRepayment(repayment);
                        log.info("MARK_REPAYMENT_LATE_SUCCESS - repaymentId: {}", repayment.getRepaymentId());
                        continue;
                    }
                    // Không đủ tiền, đánh dấu trễ và xử lý phạt
                    handleLateRepayment(loan, repayment, requiredAmount);
                }
            } catch (Exception e) {
                log.error("PROCESS_OVERDUE_REPAYMENT_ERROR - repaymentId: {}, error: {}", repayment.getRepaymentId(), e.getMessage(), e);
            }
        }
        log.info("PROCESS_OVERDUE_REPAYMENTS_SUCCESS");
    }
    /**
     * Xử lý trường hợp trễ hạn - đánh dấu trễ và cộng dồn phạt
     */
    private void handleLateRepayment(Loan loan, Repayment repayment, BigDecimal requiredAmount) {
        try {
            log.info("HANDLE_LATE_REPAYMENT_START - repaymentId: {}", repayment.getRepaymentId());
            // Đánh dấu trễ
            repayment.setStatus(RepaymentStatus.LATE);
            repaymentService.updateRepayment(repayment);
            log.info("MARK_REPAYMENT_LATE_SUCCESS - repaymentId: {}", repayment.getRepaymentId());
            // Tính số tiền chưa trả (gốc + lãi)
            BigDecimal unpaid = requiredAmount.subtract(repayment.getPaidAmount());
            // Phạt 1.5% trên tổng số tiền chưa trả
            BigDecimal penalty = unpaid.multiply(BigDecimal.valueOf(0.015)).setScale(2, BigDecimal.ROUND_HALF_UP);
            boolean isLast = repaymentService.checkLastMonthRepayment(repayment);
            if (isLast) {
                log.info("CREATE_PENALTY_REPAYMENT_START - repaymentId: {}", repayment.getRepaymentId());
                // Đã là kỳ cuối, chỉ tạo penalty mới cho tháng tiếp theo
                Repayment p = new Repayment();
                p.setLoan(repayment.getLoan());
                p.setDueDate(repayment.getDueDate().plusMonths(1));
                p.setPrincipal(repayment.getPrincipal().add(unpaid));
                p.setInterest(repayment.getInterest().add(penalty));
                p.setPaidAmount(BigDecimal.ZERO);
                p.setStatus(RepaymentStatus.UNPAID);
                repaymentService.updateRepayment(p);
                log.info("CREATE_PENALTY_REPAYMENT_SUCCESS - newDueDate: {}",
                        p.getDueDate().format(DateTimeFormatter.ISO_DATE));
            } else {
                log.info("ROLL_FORWARD_PENALTY_START - repaymentId: {}", repayment.getRepaymentId());
                Repayment current = repaymentService.getCurrentRepaymentbyLoanId(loan.getLoanId());
                if (current != null) {
                    // Cộng dồn phần chưa trả và phạt vào kỳ tiếp theo
                    current.setPrincipal(current.getPrincipal().add(unpaid));
                    current.setInterest(current.getInterest().add(penalty));
                    repaymentService.updateRepayment(current);

                    log.info("ROLL_FORWARD_PENALTY_SUCCESS - currentRepaymentId: {}", current.getRepaymentId());
                } else {
                    log.warn("ROLL_FORWARD_PENALTY_SKIPPED - no current repayment for loanId: {}",
                            loan.getLoanId());
                }
            }
            // Cập nhật dư nợ còn lại lên account loan
            BigDecimal outstandingDebt = repaymentRepository.getOutstandingDebtByLoanId(loan.getLoanId());
            updateOutstandingDebtToLoanAccount(loan.getLoanId(), outstandingDebt);
            // Gửi thông báo trễ
            sendLateRepaymentNotification(loan, repayment, unpaid, penalty);
            
            log.info("HANDLE_LATE_REPAYMENT_SUCCESS - repaymentId: {}", repayment.getRepaymentId());
        } catch (Exception e) {
            log.error("HANDLE_LATE_REPAYMENT_ERROR - repaymentId: {}, error: {}", repayment.getRepaymentId(), e.getMessage(), e);
        }
    }

    /**
     * Thực hiện tự động trừ tiền
     */
    @CachePut(value = "repaymentById", key = "#repayment.repaymentId")
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
                BigDecimal newPaidAmount = repayment.getPaidAmount().add(amount);
                repayment.setPaidAmount(newPaidAmount);
                BigDecimal totalRequired = repayment.getPrincipal().add(repayment.getInterest());
                if (newPaidAmount.compareTo(totalRequired) >= 0) {
                    repayment.setStatus(RepaymentStatus.PAID);
                } else {
                    repayment.setStatus(RepaymentStatus.PARTIAL);
                }
                repaymentService.updateRepayment(repayment);
                LoanRequestDTO updateDto = new LoanRequestDTO();
                updateDto.setLoanId(loan.getLoanId());
                updateDto.setAmount(loan.getAmount());
                updateDto.setPaidAmount(newPaidAmount);
                com.example.common_service.constant.LoanStatus mappedStatus = com.example.common_service.constant.LoanStatus.PENDING;
                if (repaymentService.checkLastMonthRepayment(repayment)) {
                    log.info("CLOSE_LOAN_AFTER_AUTO_DEDUCT - loanId: {}", loan.getLoanId());
                    loanService.closedLoan(loan.getLoanId());
                    sendLoanClosedNotification(loan, "Hoàn thành trả nợ");
                }
                updateDto.setStatus(mappedStatus);
                updateDto.setDisbursementAccountNumber(loan.getDisbursementAccountNumber());
                updateDto.setRepaymentAccountNumber(loan.getRepaymentAccountNumber());
                updateDto.setInterestRate(loan.getInterestRate());
                updateDto.setTermMonths(loan.getTermMonths());
                accountDubboService.updateAccountFromLoan(updateDto);
                // Gửi thông báo thanh toán thành công
                sendSuccessfulPaymentNotification(loan, repayment, amount);
                log.info("PERFORM_AUTO_DEDUCT_SUCCESS - loanId: {}, repaymentId: {}", loan.getLoanId(), repayment.getRepaymentId());
            } else {
                log.error("AUTO_DEDUCT_FAILED - reason: {}", tx.getFailedReason());
                // Đánh dấu trễ hạn nếu tự động trừ thất bại
                repayment.setStatus(RepaymentStatus.LATE);
                repaymentService.updateRepayment(repayment);
                
                // Gửi thông báo trễ do lỗi giao dịch
                sendLateRepaymentNotification(loan, repayment, 
                    repayment.getPrincipal().add(repayment.getInterest()).subtract(repayment.getPaidAmount()),
                    BigDecimal.ZERO);
            }
        } catch (Exception e) {
            log.error("PERFORM_AUTO_DEDUCT_ERROR - loanId: {}, repaymentId: {}, error: {}", 
                loan.getLoanId(), repayment.getRepaymentId(), e.getMessage(), e);
            // Đánh dấu trễ hạn nếu có lỗi
            repayment.setStatus(RepaymentStatus.LATE);
            repaymentService.updateRepayment(repayment);
            // Gửi thông báo trễ do lỗi hệ thống
            sendLateRepaymentNotification(loan, repayment, 
                repayment.getPrincipal().add(repayment.getInterest()).subtract(repayment.getPaidAmount()),
                BigDecimal.ZERO);
        }
    }

    /**
     * Gửi thông báo trễ hạn
     */
    private void sendLateRepaymentNotification(Loan loan, Repayment repayment, BigDecimal unpaid, BigDecimal penalty) {
        try {
            CustomerResponseDTO cust = customerQueryService.getCustomerById(loan.getCustomerId());
            String body = String.format(
                    "Kính chào %s,%n%n" +
                            "Khoản vay ID: %s (TK: %s) quá hạn từ %s.%n" +
                            "Số tiền kỳ này: %s VND.%n" +
                            "Số tiền chưa trả: %s VND.%n" +
                            "Phí phạt: %s VND.%n%n" +
                            "Vui lòng thanh toán để tránh ảnh hưởng lịch sử tín dụng.%n%n" +
                            "Trân trọng, Ngân hàng",
                    cust.getFullName(),
                    loan.getLoanId(),
                    loan.getDisbursementAccountNumber(),
                    repayment.getDueDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    repayment.getPrincipal().add(repayment.getInterest()),
                    unpaid,
                    penalty
            );
            MailMessageDTO mail = MailMessageDTO.builder()
                    .subject("THÔNG BÁO TRẢ TRễ VAY")
                    .recipient(cust.getEmail())
                    .recipientName(cust.getFullName())
                    .body(body)
                    .build();
            streamBridge.send("loan-notification-out-0", mail);
            log.info("LATE_REPAYMENT_NOTIFICATION_SENT - loanId: {}", loan.getLoanId());
        } catch (Exception e) {
            log.error("SEND_LATE_REPAYMENT_NOTIFICATION_ERROR - loanId: {}, error: {}", loan.getLoanId(), e.getMessage());
        }
    }

    /**
     * Gửi thông báo thanh toán thành công
     */
    private void sendSuccessfulPaymentNotification(Loan loan, Repayment repayment, BigDecimal amount) {
        try {
            CustomerResponseDTO cust = customerQueryService.getCustomerById(loan.getCustomerId());
            String body = String.format(
                    "Kính chào %s,%n%n" +
                            "Khoản vay ID: %s đã được thanh toán thành công.%n" +
                            "Số tiền: %s VND.%n" +
                            "Kỳ thanh toán: %s.%n%n" +
                            "Cảm ơn bạn đã sử dụng dịch vụ của chúng tôi.%n%n" +
                            "Trân trọng, Ngân hàng",
                    cust.getFullName(),
                    loan.getLoanId(),
                    amount,
                    repayment.getDueDate().format(DateTimeFormatter.ofPattern("MM/yyyy"))
            );
            MailMessageDTO mail = MailMessageDTO.builder()
                    .subject("THÔNG BÁO THANH TOÁN THÀNH CÔNG")
                    .recipient(cust.getEmail())
                    .recipientName(cust.getFullName())
                    .body(body)
                    .build();
            streamBridge.send("loan-notification-out-0", mail);
            log.info("SUCCESSFUL_PAYMENT_NOTIFICATION_SENT - loanId: {}", loan.getLoanId());
        } catch (Exception e) {
            log.error("SEND_SUCCESSFUL_PAYMENT_NOTIFICATION_ERROR - loanId: {}, error: {}", loan.getLoanId(), e.getMessage());
        }
    }

    /**
     * Gửi thông báo đóng khoản vay
     */
    private void sendLoanClosedNotification(Loan loan, String reason) {
        try {
            CustomerResponseDTO cust = customerQueryService.getCustomerById(loan.getCustomerId());
            String body = String.format(
                    "Kính chào %s,%n%n" +
                            "Khoản vay ID: %s đã được đóng.%n" +
                            "Lý do: %s.%n%n" +
                            "Vui lòng liên hệ ngân hàng để biết thêm chi tiết.%n%n" +
                            "Trân trọng, Ngân hàng",
                    cust.getFullName(),
                    loan.getLoanId(),
                    reason
            );
            MailMessageDTO mail = MailMessageDTO.builder()
                    .subject("THÔNG BÁO ĐÓNG KHOẢN VAY")
                    .recipient(cust.getEmail())
                    .recipientName(cust.getFullName())
                    .body(body)
                    .build();
            streamBridge.send("loan-notification-out-0", mail);
            log.info("LOAN_CLOSED_NOTIFICATION_SENT - loanId: {}", loan.getLoanId());
        } catch (Exception e) {
            log.error("SEND_LOAN_CLOSED_NOTIFICATION_ERROR - loanId: {}, error: {}", loan.getLoanId(), e.getMessage());
        }
    }



    /**
     * Đóng khoản vay và thu hồi tiền từ loan account
     */
    private void closeLoanAndRecover(Loan loan) {
        try {
            log.info("CLOSE_LOAN_AND_RECOVER_START - loanId: {}", loan.getLoanId());
            log.info("LOAN_CLOSED_SUCCESS - loanId: {}", loan.getLoanId());
            if (loan.getLoanType() != LoanType.PERSONAL){
                loanService.closedLoan(loan.getLoanId());
                sendLoanClosedNotification(loan, "Vi phạm điều khoản trả nợ liên tiếp");
                return;
            }
            // 2. Kiểm tra số dư loan account
            BigDecimal loanBalance = getLoanAccountBalance(loan.getDisbursementAccountNumber());
            log.info("LOAN_ACCOUNT_BALANCE - account: {}, balance: {}", loan.getDisbursementAccountNumber(), loanBalance);

            // 3. Chỉ thu hồi nếu có tài sản
            if (loanBalance.compareTo(BigDecimal.ZERO) > 0  && loan.getLoanType() == LoanType.PERSONAL ) {
                log.info("RECOVER_LOAN_AMOUNT - amount: {}", loanBalance);
                CommonDisburseRequest recoverRequest = new CommonDisburseRequest();
                recoverRequest.setToAccountNumber(loan.getDisbursementAccountNumber());
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

            loanService.cancelledLoan(loan.getLoanId());
            LoanRequestDTO updateDto = new LoanRequestDTO();
            updateDto.setLoanId(loan.getLoanId());
            updateDto.setAmount(BigDecimal.ZERO); // Số dư = 0 sau khi thu hồi
            updateDto.setStatus(LoanStatus.CANCELLED);
            updateDto.setPaidAmount(BigDecimal.ZERO);
            updateDto.setDisbursementAccountNumber(loan.getDisbursementAccountNumber());
            updateDto.setRepaymentAccountNumber(loan.getRepaymentAccountNumber());
            updateDto.setInterestRate(loan.getInterestRate());
            updateDto.setTermMonths(loan.getTermMonths());
            accountDubboService.updateAccountFromLoan(updateDto);
            
            // 5. Cập nhật core banking
            CoreAccountRequest coreAccountRequest = CoreAccountMapper.INSTANCE.fromLoan(loan);
            coreAccountRequest.setBalance(BigDecimal.ZERO);
            coreAccountRequest.setStatus(AccountStatus.CLOSED);
            coreBankingClient.updateAccount(coreAccountRequest);
            
            // 6. Gửi thông báo đóng khoản vay
            sendLoanClosedNotification(loan, "Vi phạm điều khoản trả nợ liên tiếp");
            
            log.info("CLOSE_LOAN_AND_RECOVER_SUCCESS - loanId: {}", loan.getLoanId());
        } catch (Exception e) {
            log.error("CLOSE_LOAN_AND_RECOVER_ERROR - loanId: {}, error: {}", loan.getLoanId(), e.getMessage(), e);
            throw new RuntimeException("Failed to close loan and recover: " + e.getMessage(), e);
        }
    }

    /**
     * Lấy số dư của loan account
     */
    private BigDecimal getLoanAccountBalance(String accountNumber) {
        try {
            // Gọi trực tiếp đến Core Banking Service để lấy số dư chính xác
            BigDecimal balance = coreBankingClient.getBalance(accountNumber);
            log.info("GET_LOAN_ACCOUNT_BALANCE_FROM_CORE_BANKING - account: {}, balance: {}", accountNumber, balance);
            return balance;
        } catch (Exception e) {
            log.error("GET_LOAN_ACCOUNT_BALANCE_ERROR - account: {}, error: {}", accountNumber, e.getMessage());
            
            // Nếu có lỗi khi gọi Core Banking, thử lấy từ account service
            try {
                AccountDTO account = accountQueryService.getAccountByAccountNumber(accountNumber);
                if (account != null && account.getBalance() != null) {
                    log.info("GET_LOAN_ACCOUNT_BALANCE_FALLBACK_TO_ACCOUNT_SERVICE - account: {}, balance: {}", accountNumber, account.getBalance());
                    return account.getBalance();
                }
            } catch (Exception ex) {
                log.error("GET_LOAN_ACCOUNT_BALANCE_FALLBACK_ERROR - account: {}, error: {}", accountNumber, ex.getMessage());
            }
            
            // Nếu không lấy được từ cả hai nguồn, trả về 0
            log.warn("GET_LOAN_ACCOUNT_BALANCE_NOT_FOUND - account: {}, using zero balance", accountNumber);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Lấy số dư của repayment account
     */
    private BigDecimal getRepaymentAccountBalance(String accountNumber) {
        try {
            // Gọi trực tiếp đến Core Banking Service để lấy số dư chính xác
            BigDecimal balance = coreBankingClient.getBalance(accountNumber);
            log.info("GET_REPAYMENT_ACCOUNT_BALANCE_FROM_CORE_BANKING - account: {}, balance: {}", accountNumber, balance);
            return balance;
        } catch (Exception e) {
            log.error("GET_REPAYMENT_ACCOUNT_BALANCE_ERROR - account: {}, error: {}", accountNumber, e.getMessage());
            
            // Nếu có lỗi khi gọi Core Banking, thử lấy từ account service
            try {
                AccountDTO account = accountQueryService.getAccountByAccountNumber(accountNumber);
                if (account != null && account.getBalance() != null) {
                    log.info("GET_REPAYMENT_ACCOUNT_BALANCE_FALLBACK_TO_ACCOUNT_SERVICE - account: {}, balance: {}", accountNumber, account.getBalance());
                    return account.getBalance();
                }
            } catch (Exception ex) {
                log.error("GET_REPAYMENT_ACCOUNT_BALANCE_FALLBACK_ERROR - account: {}, error: {}", accountNumber, ex.getMessage());
            }
            
            // Nếu không lấy được từ cả hai nguồn, trả về 0
            log.warn("GET_REPAYMENT_ACCOUNT_BALANCE_NOT_FOUND - account: {}, using zero balance", accountNumber);
            return BigDecimal.ZERO;
        }
    }


    @Transactional
//    @Scheduled(cron = "0 0 3 * * ?")
//    @Scheduled(fixedRate = 60 * 60 * 1000)
    public void remindUpcomingRepayments() {
        long startTime = System.currentTimeMillis();
        log.info("REMIND_UPCOMING_REPAYMENTS_START -");

        try {
            List<Repayment> upcoming = repaymentService.findAfter3DaysNative();
            log.info("REMIND_UPCOMING_REPAYMENTS_FOUND - count: {}", upcoming.size());

            for (Repayment next : upcoming) {
                try {
                    Long loanId = next.getLoan().getLoanId();
                    Long customerId = next.getLoan().getCustomerId();
                    LocalDate dueDate = next.getDueDate();
                    BigDecimal totalAmount = next.getPrincipal().add(next.getInterest());
                    log.info("REMIND_UPCOMING_REPAYMENT_DETAIL - repaymentId: {}, loanId: {}, customerId: {}, dueDate: {}, amount: {}",
                            next.getRepaymentId(), loanId, customerId, dueDate, totalAmount);
                    CustomerResponseDTO cust = customerQueryService.getCustomerById(customerId);
                    String body = String.format(
                            "Kính chào %s,%n%n" +
                                    "Bạn còn %d ngày đến kỳ thanh toán khoản vay ID: %s với tổng số tiền %s VND.%n" +
                                    "Ngày đến hạn: %s.%n%n" +
                                    "Vui lòng chuẩn bị để tránh phát sinh phí trễ.%n%n" +
                                    "Trân trọng, Ngân hàng",
                            cust.getFullName(),
                            ChronoUnit.DAYS.between(LocalDate.now(), dueDate),
                            loanId,
                            totalAmount,
                            dueDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    );

                    MailMessageDTO mail = MailMessageDTO.builder()
                            .subject("NHẮC NỢ ĐỊNH KỲ")
                            .recipient(cust.getEmail())
                            .recipientName(cust.getFullName())
                            .body(body)
                            .build();

                    log.info("REMIND_UPCOMING_REPAYMENT_MAIL_PREPARED - to: {}, name: {}, subject: {}",
                            mail.getRecipient(), mail.getRecipientName(), mail.getSubject());

                    streamBridge.send("loan-notification-out-0", mail);
                    log.info("REMIND_UPCOMING_REPAYMENT_NOTIFICATION_SENT - repaymentId: {}, loanId: {}, email: {}",
                            next.getRepaymentId(), loanId, cust.getEmail());

                } catch (Exception e) {
                    log.info("REMIND_UPCOMING_REPAYMENT_ERROR - repaymentId: {}, loanId: {}, error: {}",
                            next.getRepaymentId(), next.getLoan().getLoanId(), e.getMessage(), e);
                }
            }
            log.info("REMIND_UPCOMING_REPAYMENTS_SUCCESS - processed: {}, durationMs: {}",
                    upcoming.size(), System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            log.info("REMIND_UPCOMING_REPAYMENTS_FATAL_ERROR - error: {}", e.getMessage(), e);
        }
    }


    /**
     * Cập nhật dư nợ cho loan account thông qua Dubbo (chỉ dư nợ)
     */
    private void updateOutstandingDebtToLoanAccount(Long loanId, BigDecimal outstandingDebt) {
        accountDubboService.updateOutstandingDebt(loanId, outstandingDebt);
        log.info("UPDATE_OUTSTANDING_DEBT_TO_LOAN_ACCOUNT - loanId: {}, amount: {}", loanId, outstandingDebt);
    }
}
