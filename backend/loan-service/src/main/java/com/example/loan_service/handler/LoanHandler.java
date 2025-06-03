package com.example.loan_service.handler;


import com.example.common_service.dto.AccountDTO;
import com.example.common_service.dto.CommonTransactionDTO;
import com.example.common_service.dto.CustomerResponseDTO;
import com.example.common_service.constant.CustomerStatus;
import com.example.common_service.dto.MailMessageDTO;
import com.example.common_service.dto.request.CommonConfirmTransactionRequest;
import com.example.common_service.dto.request.CommonDepositRequest;
import com.example.common_service.dto.request.CommonDisburseRequest;
import com.example.common_service.dto.request.PayRepaymentRequest;
import com.example.common_service.models.KycStatus;
import com.example.common_service.services.account.AccountQueryService;
import com.example.common_service.services.customer.CustomerQueryService;
import com.example.common_service.services.customer.CustomerService;
import com.example.common_service.services.transactions.CommonTransactionService;
import com.example.loan_service.entity.Loan;
import com.example.loan_service.entity.Repayment;
import com.example.loan_service.mapper.LoanMapper;
import com.example.loan_service.mapper.RepaymentMapper;
import com.example.loan_service.models.RepaymentStatus;
import com.example.loan_service.service.CoreBankingClient;
import com.example.loan_service.service.LoanService;
import com.example.loan_service.service.RepaymentService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.checkerframework.checker.units.qual.C;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LoanHandler {
    private final StreamBridge streamBridge;
    private final LoanService loanService;
    private final LoanMapper loanMapper;
    private final CoreBankingClient coreBankingClient;
    private final RepaymentService repaymentService;
    @DubboReference
    private final CustomerQueryService customerQueryService;
    @DubboReference
    private final AccountQueryService accountQueryService;
    @DubboReference
    private final CommonTransactionService commonTransactionService;
    public Loan createLoan(Loan loan) throws  Exception {
        CustomerResponseDTO customer = customerQueryService.getCustomerById(loan.getCustomerId());
        System.out.println(customer.getDateOfBirth());
        AccountDTO account = accountQueryService.getAccountByAccountNumber(loan.getAccountNumber());
        if (!customer.getStatus().equals(CustomerStatus.ACTIVE)){
            throw new IllegalArgumentException("Customer status is not  ACTIVE");
        } else if (Period.between(customer.getDateOfBirth(), LocalDate.now()).getYears() <= 18) {
            throw new IllegalArgumentException("Customer is not old enough");
        } else if (!account.getStatus().equalsIgnoreCase("ACTIVE")) {
            throw new IllegalArgumentException("Account status is not  ACTIVE");
        }else if (loan.getDeclaredIncome().compareTo(BigDecimal.valueOf(8000000.00)) <0){
            throw new IllegalArgumentException("Declared is not enough");
        }else{
            coreBankingClient.syncLoan(loanMapper.toRequestDTO(loan));
            return loanService.createLoan(loan);
        }
    }

    public Loan updateLoan(Loan loan) {
        return loanService.updateLoan(loan);
    }

    public Loan approveLoan(Long loanId) {
        Loan loan =  loanService.getLoanById(loanId).orElse(null);
        CommonDisburseRequest commonDisburseRequest = new CommonDisburseRequest();
        commonDisburseRequest.setToAccountNumber(loan.getAccountNumber());
        commonDisburseRequest.setAmount(loan.getAmount());
        commonDisburseRequest.setCurrency("VND");
        CommonTransactionDTO transaction = commonTransactionService.loanDisbursement(commonDisburseRequest);
        if (!transaction.getStatus().equalsIgnoreCase("COMPLETED")) {
            throw new IllegalArgumentException(transaction.getFailedReason());
        }else {
            System.out.println(transaction);
            try {
                loan = loanService.approveLoan(loanId);
                repaymentService.generateRepaymentSchedule(loan);
                coreBankingClient.syncLoan(loanMapper.toRequestDTO(loan));
                CustomerResponseDTO customer = customerQueryService.getCustomerById(loan.getCustomerId());
                MailMessageDTO mailMessage = new MailMessageDTO();
                mailMessage.setSubject("KÍCH HOẠT KHOẢN VAY");
                mailMessage.setRecipient("phanhuynhphuckhang12c8@gmail.com");
                mailMessage.setBody("Khoản vay của bạn đã được duyệt thành công và giải ngân đến tài khoản: "+loan.getAccountNumber());
                mailMessage.setRecipientName(customer.getFullName());
                streamBridge.send("mail-out-0", mailMessage);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return loan;
    }

    public Optional<Loan> getLoanById(Long loanId) {
        return loanService.getLoanById(loanId);
    }

    public List<Loan> getLoansByCustomerId(Long customerId) {
        return loanService.getLoansByCustomerId(customerId);
    }

    public Loan closedLoan(Long loanId) {
        Loan loan =  new Loan();
        try {
            loan = loanService.closedLoan(loanId);
            coreBankingClient.syncLoan(loanMapper.toRequestDTO(loan));
        }catch (Exception e) {
            e.printStackTrace();
        }
        return loan;
    }

    public Loan rejectedLoan(Long loanId) {
        Loan loan =  new Loan();
        try {
            loan = loanService.rejectedLoan(loanId);
            coreBankingClient.syncLoan(loanMapper.toRequestDTO(loan));
        }catch (Exception e) {
            e.printStackTrace();
        }
        return loan;
    }
    public List<Loan> findall(){
        return loanService.findAllLoan();
    }
    public void deleteLoan(Long loanId) {
        loanService.deleteLoan(loanId);
        coreBankingClient.deleteLoan(loanId);
    }

    public List<Repayment> getRepaymentsByLoanId(Long loanId) {
        return repaymentService.getRepaymentsByLoanId(loanId);
    }

    public Boolean makeRepayment(Long repaymentId, java.math.BigDecimal amount) {
        PayRepaymentRequest pay = new PayRepaymentRequest();
        pay.setAmount(amount);
        pay.setCurrency("VND");
        pay.setDescription("Get transaction, sent otp");
        Repayment repayment = repaymentService.getRepaymentById(repaymentId).orElse(null);
        System.out.println("ssssssssssssssssssssssssssssssssssssssss");
        pay.setFromAccountNumber(repayment.getLoan().getAccountNumber());
        System.out.println("ssssssssssssssssssssssssssssssssssssssss");
        CommonTransactionDTO transaction = commonTransactionService.loanPayment(pay);
        System.out.println(transaction);
        if (!transaction.getStatus().equalsIgnoreCase("PENDING")) {
            throw new IllegalArgumentException(transaction.getFailedReason());
        }
        return true;
    }
    public Repayment confirmRepayment(Long repaymentId, java.math.BigDecimal amount,String otpCode,String referenceCode) {
        CommonConfirmTransactionRequest confirm = new CommonConfirmTransactionRequest();
        confirm.setOtpCode(otpCode);
        confirm.setReferenceCode(referenceCode);
        CommonTransactionDTO transaction = commonTransactionService.confirmTransaction(confirm);
        if (!transaction.getStatus().equalsIgnoreCase("COMPLETED")) {
            throw new IllegalArgumentException(transaction.getFailedReason());
        }
        return repaymentService.makeRepayment(repaymentId, amount);
    }

    public List<Repayment> getHistory(Long loanId) { return repaymentService.getHistoryRepayment(loanId);}

    public Repayment getCurrentRepayment(Long loanId) { return repaymentService.getCurrentRepayment(loanId);}

    public Optional<Repayment> getRepaymentById(Long repaymentId) {
        return repaymentService.getRepaymentById(repaymentId);
    }

    public Repayment unpaidRepayment(Long repaymentId) {
        return repaymentService.updateRepaymentStatus(repaymentId, RepaymentStatus.UNPAID);
    }

    public Repayment lateRepayment(Long repaymentId) {
        return repaymentService.updateRepaymentStatus(repaymentId, RepaymentStatus.LATE);
    }

    public void deleteRepaymentsByLoanId(Long loanId) {
        repaymentService.deleteRepaymentsByLoanId(loanId);
    }
}
