package com.example.loan_service.scheduler;

import com.example.common_service.dto.CustomerResponseDTO;
import com.example.common_service.dto.MailMessageDTO;
import com.example.common_service.services.customer.CustomerQueryService;
import com.example.loan_service.entity.Loan;
import com.example.loan_service.entity.Repayment;
import com.example.loan_service.mapper.LoanMapper;
import com.example.loan_service.models.RepaymentStatus;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class RepaymentCheckScherduler {

    private final LoanService loanService;
    private final LoanMapper loanMapper;
    private final StreamBridge streamBridge;
    private final CoreBankingClient coreBankingClient;
    private final RepaymentService repaymentService;
    @DubboReference private final CustomerQueryService customerQueryService;

    @Scheduled(fixedRateString = "${repayment.scheduler.fix-rate:5000000}")
    public void checkAndHandleLateRepayments() {
        log.info("CHECK_LATE_REPAYMENTS_START");
        try {
            List<Loan> approvedLoans = loanService.getLoansApprove();
            log.info("CHECK_LATE_REPAYMENTS - loansFetched: {}", approvedLoans.size());

            for (Loan loan : approvedLoans) {
                log.debug("CHECK_LATE_REPAYMENTS_PROCESS_LOAN - loanId: {}", loan.getLoanId());
                List<Repayment> dueList = repaymentService.getRepaymentNotPaid(loan.getLoanId());
                for (Repayment repayment : dueList) {
                    if (repayment.getDueDate().isBefore(LocalDate.now())
                            && repayment.getStatus() != RepaymentStatus.LATE) {
                        log.info("MARK_REPAYMENT_LATE_START - repaymentId: {}", repayment.getRepaymentId());

                        // đánh dấu trễ
                        repayment.setStatus(RepaymentStatus.LATE);
                        repaymentService.updateRepayment(repayment);
                        log.info("MARK_REPAYMENT_LATE_SUCCESS - repaymentId: {}", repayment.getRepaymentId());

                        boolean isLast = repaymentService.checkLastMonthRepayment(repayment);
                        if (isLast) {
                            log.info("CREATE_PENALTY_REPAYMENT_START - repaymentId: {}", repayment.getRepaymentId());
                            Repayment penalty = new Repayment();
                            penalty.setPrincipal(repayment.getPrincipal());
                            penalty.setStatus(RepaymentStatus.UNPAID);
                            penalty.setDueDate(repayment.getDueDate().plusMonths(1));
                            penalty.setInterest(
                                repayment.getInterest()
                                         .add(repayment.getInterest().multiply(BigDecimal.valueOf(0.015)))
                            );
                            penalty.setLoan(repayment.getLoan());
                            penalty.setPaidAmount(BigDecimal.ZERO);
                            repaymentService.updateRepayment(penalty);
                            coreBankingClient.syncLoan(loanMapper.toResponseDTO(loanMapper.toRequestDTO(loan)));
                            log.info("CREATE_PENALTY_REPAYMENT_SUCCESS - newRepaymentDueDate: {}",
                                    penalty.getDueDate().format(DateTimeFormatter.ISO_DATE));
                        } else {
                            log.info("ROLL_FORWARD_PENALTY_START - repaymentId: {}", repayment.getRepaymentId());
                            Repayment current = repaymentService.getCurrentRepaymentbyLoanId(loan.getLoanId());
                            if (current != null) {
                                BigDecimal extraPrincipal = repayment.getPrincipal()
                                        .add(repayment.getInterest())
                                        .subtract(repayment.getPaidAmount());
                                current.setPrincipal(current.getPrincipal().add(extraPrincipal));
                                current.setInterest(
                                    current.getInterest()
                                           .add(repayment.getInterest().multiply(BigDecimal.valueOf(0.015)))
                                );
                                repaymentService.updateRepayment(current);
                                coreBankingClient.syncLoan(loanMapper.toResponseDTO(loanMapper.toRequestDTO(loan)));
                                log.info("ROLL_FORWARD_PENALTY_SUCCESS - currentRepaymentId: {}", current.getRepaymentId());
                            } else {
                                log.warn("ROLL_FORWARD_PENALTY_SKIPPED - no current repayment for loanId: {}", loan.getLoanId());
                            }
                        }

                        // gửi email thông báo
                        CustomerResponseDTO customer = customerQueryService.getCustomerById(loan.getCustomerId());
                        MailMessageDTO mail = new MailMessageDTO();
                        mail.setSubject("THÔNG BÁO TRẢ TRỄ VAY");
                        mail.setRecipient("phanhuynhphuckhang12c8@gmail.com");
                        String body = String.format(
                            "Kính chào %s,%n%n" +
                            "Khoản vay ID: %s (TK: %s) quá hạn từ %s.%n" +
                            "Số tiền kỳ này: %s VND.%n%n" +
                            "Vui lòng thanh toán để tránh ảnh hưởng lịch sử tín dụng.%n%n" +
                            "Trân trọng, Ngân hàng",
                            customer.getFullName(),
                            loan.getLoanId(),
                            loan.getAccountNumber(),
                            repayment.getDueDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                            repayment.getPrincipal().add(repayment.getInterest())
                        );
                        mail.setBody(body);
                        mail.setRecipientName(customer.getFullName());
                        streamBridge.send("mail-out-0", mail);
                        log.info("LATE_REPAYMENT_NOTIFICATION_SENT - loanId: {}", loan.getLoanId());

                        break;  // chỉ xử lý 1 kỳ trễ mỗi loan lần chạy
                    }
                }
            }
            log.info("CHECK_LATE_REPAYMENTS_SUCCESS");
        } catch (Exception e) {
            log.error("CHECK_LATE_REPAYMENTS_ERROR - error: {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "${repayment.scheduler.remind-cron:0 0 9 * * *}")
    public void remindUpcomingRepayments() {
        log.info("REMIND_UPCOMING_REPAYMENTS_START");
        try {
            List<Loan> approvedLoans = loanService.getLoansApprove();
            log.info("REMIND_UPCOMING_REPAYMENTS - loansFetched: {}", approvedLoans.size());

            for (Loan loan : approvedLoans) {
                Repayment next = repaymentService.getCurrentRepaymentbyLoanId(loan.getLoanId());
                if (next != null) {
                    long daysToDue = ChronoUnit.DAYS.between(LocalDate.now(), next.getDueDate());
                    if (daysToDue >= 1 && daysToDue <= 3) {
                        log.info("REMIND_UPCOMING_REPAYMENT_PROCESS - repaymentId: {}, daysToDue: {}",
                                next.getRepaymentId(), daysToDue);

                        CustomerResponseDTO customer = customerQueryService.getCustomerById(loan.getCustomerId());
                        MailMessageDTO mail = new MailMessageDTO();
                        mail.setSubject("NHẮC NỢ ĐỊNH KỲ");
                        mail.setRecipient("phanhuynhphuckhang12c8@gmail.com");
                        mail.setBody(String.format(
                            "Kính chào %s,%n%n" +
                            "Bạn còn %d ngày đến kỳ thanh toán khoản vay ID: %s với tổng số tiền %s VND.%n%n" +
                            "Vui lòng chuẩn bị để tránh phát sinh phí trễ.%n%n" +
                            "Trân trọng, Ngân hàng",
                            customer.getFullName(),
                            daysToDue,
                            loan.getLoanId(),
                            next.getPrincipal().add(next.getInterest())
                        ));
                        mail.setRecipientName(customer.getFullName());
                        streamBridge.send("mail-out-0", mail);
                        log.info("REMIND_UPCOMING_REPAYMENT_NOTIFICATION_SENT - repaymentId: {}", next.getRepaymentId());
                    }
                }
            }
            log.info("REMIND_UPCOMING_REPAYMENTS_SUCCESS");
        } catch (Exception e) {
            log.error("REMIND_UPCOMING_REPAYMENTS_ERROR - error: {}", e.getMessage(), e);
        }
    }
}
