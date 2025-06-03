package com.example.loan_service.scheduler;

import com.example.common_service.dto.AccountDTO;
import com.example.common_service.dto.CustomerResponseDTO;
import com.example.common_service.dto.MailMessageDTO;
import com.example.common_service.services.account.AccountQueryService;
import com.example.common_service.services.customer.CustomerQueryService;
import com.example.loan_service.entity.Loan;
import com.example.loan_service.entity.Repayment;
import com.example.loan_service.mapper.LoanMapper;
import com.example.loan_service.models.RepaymentStatus;
import com.example.loan_service.service.CoreBankingClient;
import com.example.loan_service.service.LoanService;
import com.example.loan_service.service.RepaymentService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RepaymentCheckScherduler {
    private final LoanService loanService;
    private final LoanMapper loanMapper;
    private final StreamBridge streamBridge;
    private final CoreBankingClient coreBankingClient;
    private final RepaymentService repaymentService;
    @DubboReference
    private final CustomerQueryService customerQueryService;
    @DubboReference
    private final AccountQueryService accountQueryService;
//    @Scheduled(fixedRate = 5000)
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
                    coreBankingClient.syncLoan(loanMapper.toRequestDTO(loan));
                    break;
                }
            }
        }

    }
//        @Scheduled(fixedRate = 5000)
//    @Scheduled(cron = "0 0 1 * * *")
    public void RepaymentRemindScherduler() {
        List<Loan> loanApproved = loanService.getLoansApprove();
        for (Loan loan : loanApproved) {
            System.out.println("Kiểm tra loan: "+loan.getLoanId());
            Repayment repayment =repaymentService.getCurrentRepayment(loan.getLoanId());
            //tim ra khoan vay dinh ky con 3 ngay nua toi han dong
            if (repayment.getDueDate().compareTo(LocalDate.now()) >= 3 ) {
                CustomerResponseDTO customer = customerQueryService.getCustomerById(loan.getCustomerId());
                MailMessageDTO mailMessage = new MailMessageDTO();
                mailMessage.setSubject("NHẮC NỢ ĐỊNH KỲ");
                mailMessage.setRecipient("phanhuynhphuckhang12c8@gmail.com");
                mailMessage.setBody("Bạn sắp đến hạn thanh toán vay nợ định kỳ. Khoản vay: "+repayment.getPrincipal().add(repayment.getInterest())+", Đến số tài khoản: "+loan.getAccountNumber());
                mailMessage.setRecipientName(customer.getFullName());
                streamBridge.send("mail-out-0", mailMessage);
                break;
            }
        }
    }
}
