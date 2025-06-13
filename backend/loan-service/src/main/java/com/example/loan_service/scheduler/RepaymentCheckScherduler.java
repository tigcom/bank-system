package com.example.loan_service.scheduler;

import com.example.loan_service.entity.Loan;
import com.example.loan_service.entity.Repayment;
import com.example.loan_service.models.RepaymentStatus;
import com.example.loan_service.service.LoanService;
import com.example.loan_service.service.RepaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RepaymentCheckScherduler {
    private final LoanService loanService;
    private final RepaymentService repaymentService;
//    @Scheduled(fixedRate = 5000)
//    @Scheduled(cron = "0 0 9 * * *")
//    @Scheduled(cron = "0 0 1 * * *")
    public void RepaymentCheckScherduler() {
        List<Loan> loanApproved = loanService.getLoansApprove();
        for (Loan loan : loanApproved) {
            System.out.println("Kiểm tra loan: "+loan.getLoanId());
            List<Repayment> repayments =repaymentService.getRepaymentNotPaid(loan.getLoanId());
            for (Repayment repayment : repayments) {
                //tim ra khoan vay dinh ky dang bi tre so voi ngay hien tai
                if (repayment.getDueDate().isBefore(LocalDate.now()) && repayment.getStatus() != RepaymentStatus.LATE ) {
                    System.out.println("Đã tìm thấy khoan vay bị trễ"+repayment.getRepaymentId());
                    repayment.setStatus(RepaymentStatus.LATE);
                    repaymentService.updateRepayment(repayment);
                    // tim ra khoan vay hien tai
                    Repayment currentRepayment =
                            repaymentService.getCurrentRepayment(
                                    repayment.getLoan().getLoanId());
                    // + khoan vay goc dot truoc vao hien tai
                    currentRepayment.setPrincipal(currentRepayment.getPrincipal()
                            .add(repayment.getPrincipal().add(repayment.getInterest()).subtract(repayment.getPaidAmount())));
                    // + lai vay *1.5% dot truoc vao hien tai
                    currentRepayment.setInterest(currentRepayment.getInterest()
                            .add(repayment.getInterest()
                            .multiply(BigDecimal.valueOf(0.015))));
                    System.out.println("Cập nhập khoản vay kỳ hiện tại sau kỳ trễ"+currentRepayment.getRepaymentId());
                    repaymentService.updateRepayment(currentRepayment);
                    break;
                }
            }
        }

    }
}
