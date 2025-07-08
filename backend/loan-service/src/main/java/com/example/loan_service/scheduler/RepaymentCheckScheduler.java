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
public class RepaymentCheckScheduler {

    private final LoanService loanService;
    private final RepaymentService repaymentService;
    private final LoanMapper loanMapper;
    private final CoreBankingClient coreBankingClient;
    private final StreamBridge streamBridge;
    @DubboReference private final CustomerQueryService customerQueryService;

    @Scheduled(fixedRateString = "${repayment.scheduler.fix-rate:5000000}")
    public void checkAndHandleLateRepayments() {
        log.info("CHECK_LATE_REPAYMENTS_START");
        for (Loan loan : loanService.getLoansApprove()) {
            List<Repayment> overdue = repaymentService.getOverdueRepayments(loan.getLoanId());
            if (overdue.isEmpty()) continue;

            Repayment r = overdue.get(0);
            log.info("MARK_REPAYMENT_LATE_START - repaymentId: {}", r.getRepaymentId());

            // đánh dấu trễ
            r.setStatus(RepaymentStatus.LATE);
            repaymentService.updateRepayment(r);
            log.info("MARK_REPAYMENT_LATE_SUCCESS - repaymentId: {}", r.getRepaymentId());

            boolean isLast = repaymentService.checkLastMonthRepayment(r);
            if (isLast) {
                log.info("CREATE_PENALTY_REPAYMENT_START - repaymentId: {}", r.getRepaymentId());
                Repayment p = new Repayment();
                p.setLoan(r.getLoan());
                p.setDueDate(r.getDueDate().plusMonths(1));
                p.setPrincipal(r.getPrincipal());
                p.setInterest(r.getInterest()
                        .add(r.getInterest().multiply(BigDecimal.valueOf(0.015))));
                p.setPaidAmount(BigDecimal.ZERO);
                p.setStatus(RepaymentStatus.UNPAID);
                repaymentService.updateRepayment(p);
                coreBankingClient.syncLoan(
                        loanMapper.toResponseDTO(loanMapper.toRequestDTO(loan))
                );
                log.info("CREATE_PENALTY_REPAYMENT_SUCCESS - newDueDate: {}",
                        p.getDueDate().format(DateTimeFormatter.ISO_DATE));
            } else {
                log.info("ROLL_FORWARD_PENALTY_START - repaymentId: {}", r.getRepaymentId());
                Repayment current = repaymentService.getCurrentRepaymentbyLoanId(loan.getLoanId());
                if (current != null) {
                    BigDecimal extra = r.getPrincipal()
                            .add(r.getInterest())
                            .subtract(r.getPaidAmount());
                    current.setPrincipal(current.getPrincipal().add(extra));
                    current.setInterest(
                            current.getInterest()
                                    .add(r.getInterest().multiply(BigDecimal.valueOf(0.015)))
                    );
                    repaymentService.updateRepayment(current);
                    coreBankingClient.syncLoan(
                            loanMapper.toResponseDTO(loanMapper.toRequestDTO(loan))
                    );
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
                    loan.getAccountNumber(),
                    r.getDueDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    r.getPrincipal().add(r.getInterest())
            );
            MailMessageDTO mail = MailMessageDTO.builder()
                    .subject("THÔNG BÁO TRẢ TRỄ VAY")
                    .recipient(cust.getEmail())
                    .recipientName(cust.getFullName())
                    .body(body)
                    .build();
            streamBridge.send("mail-out-0", mail);
            log.info("LATE_REPAYMENT_NOTIFICATION_SENT - loanId: {}", loan.getLoanId());
        }
        log.info("CHECK_LATE_REPAYMENTS_SUCCESS");
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

            log.info("REMIND_UPCOMING_REPAYMENT_PROCESS - repaymentId: {}, daysToDue: {}",
                    next.getRepaymentId(), daysToDue);

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
}
