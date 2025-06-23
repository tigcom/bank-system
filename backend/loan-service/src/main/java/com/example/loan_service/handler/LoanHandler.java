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
import com.example.common_service.services.CommonService;
import com.example.common_service.services.account.AccountQueryService;
import com.example.common_service.services.customer.CustomerQueryService;
import com.example.common_service.services.customer.CustomerService;
import com.example.common_service.services.transactions.CommonTransactionService;
import com.example.loan_service.dto.request.LoanRejectionReasonRequestDTO;
import com.example.loan_service.dto.request.LoanRequestDTO;
import com.example.loan_service.entity.Loan;
import com.example.loan_service.entity.LoanRejectionReason;
import com.example.loan_service.entity.Repayment;
import com.example.loan_service.mapper.LoanMapper;
import com.example.loan_service.mapper.RepaymentMapper;
import com.example.loan_service.models.RepaymentStatus;
import com.example.loan_service.service.CoreBankingClient;
import com.example.loan_service.service.LoanRejectionReasonService;
import com.example.loan_service.service.LoanService;
import com.example.loan_service.service.RepaymentService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.rpc.RpcContext;
import org.checkerframework.checker.units.qual.C;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
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
    private final LoanRejectionReasonService loanRejectionReasonService;
    @DubboReference
    private final CustomerQueryService customerQueryService;
    @DubboReference
    private final AccountQueryService accountQueryService;
    @DubboReference
    private final CommonTransactionService commonTransactionService;
    @DubboReference
    private final CommonService commonService;

    public Loan approveLoan(Long loanId) {
        Long idCustomer = getCustomerId();

        System.out.println(loanId);
        System.out.println(idCustomer);
        Loan loan = loanService.getLoanById(loanId).orElse(null);
        loan.setCustomerId(idCustomer);
        System.out.println(loan.getAmount());
        CommonDisburseRequest commonDisburseRequest = new CommonDisburseRequest();
        commonDisburseRequest.setToAccountNumber(loan.getAccountNumber());
        commonDisburseRequest.setAmount(loan.getAmount());
        commonDisburseRequest.setCurrency("VND");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) auth;
        String token = jwtAuth.getToken().getTokenValue();
        System.out.println("ssssssssssssssss");
        System.out.println(token);
        RpcContext.getContext()
                .setAttachment("security_jwt_token", token);

        CommonTransactionDTO transaction = commonTransactionService.loanDisbursement(commonDisburseRequest);
        SecurityContextHolder.clearContext();
        System.out.println("transaction");
        if (!transaction.getStatus().equalsIgnoreCase("COMPLETED")) {
            throw new IllegalArgumentException(transaction.getFailedReason());
        } else {
            System.out.println(transaction);
            try {
                loan = loanService.approveLoan(loanId);
                repaymentService.generateRepaymentSchedule(loan);
                coreBankingClient.syncLoan(loanMapper.toDTO(loan));

                CustomerResponseDTO customer = customerQueryService.getCustomerById(loan.getCustomerId());
                MailMessageDTO mailMessage = new MailMessageDTO();
                mailMessage.setSubject("KÍCH HOẠT KHOẢN VAY");
                mailMessage.setRecipient("phanhuynhphuckhang12c8@gmail.com");
                mailMessage.setBody("Khoản vay của bạn đã được duyệt thành công và giải ngân đến tài khoản: " + loan.getAccountNumber());
                mailMessage.setRecipientName(customer.getFullName());
                streamBridge.send("mail-out-0", mailMessage);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return loan;
    }
    public Loan createLoan(LoanRequestDTO loan) throws Exception {
        System.out.println(loan);
        Long idCustomer = getCustomerId();

        CustomerResponseDTO customer = customerQueryService.getCustomerById(idCustomer);
        System.out.println(customer.getDateOfBirth());
        AccountDTO account = accountQueryService.getAccountByAccountNumber(loan.getAccountNumber());
        System.out.println(customer);
        if (!customer.getStatus().equals(CustomerStatus.ACTIVE)) {
            throw new IllegalArgumentException("Customer status is not ACTIVE");
        } else if (Period.between(customer.getDateOfBirth(), LocalDate.now()).getYears() <= 18) {
            throw new IllegalArgumentException("Customer is not old enough");
        } else if (!account.getStatus().equalsIgnoreCase("ACTIVE")) {
            throw new IllegalArgumentException("Account status is not ACTIVE");
        } else if (loan.getDeclaredIncome().compareTo(BigDecimal.valueOf(5_000_000.00)) < 0) {
            throw new IllegalArgumentException("Declared income is not enough");
        } else {
            coreBankingClient.syncLoan(loanMapper.toResponseDTO(loan));
            Loan l = loanMapper.toEntity(loan);
            l.setCustomerId(idCustomer);
            System.out.println(l);
            return loanService.createLoan(l);
        }
    }

    public Loan updateLoan(LoanRequestDTO loan) {
        Long idCustomer = getCustomerId();
        Loan l = loanMapper.toEntity(loan);
        l.setCustomerId(idCustomer);
        return loanService.updateLoan(l);
    }


    public Optional<Loan> getLoanById(Long loanId) {
        return loanService.getLoanById(loanId);
    }

    public List<Loan> getLoansByCustomerId() {
        return loanService.getLoansByCustomerId(this.getCustomerId());
    }

    public Loan closedLoan(Long loanId) {
        Loan loan = new Loan();
        try {
            loan = loanService.closedLoan(loanId);
            Long idCustomer = getCustomerId();
            loan.setCustomerId(idCustomer);
            coreBankingClient.syncLoan(loanMapper.toDTO(loan));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return loan;
    }

    public Loan rejectedLoan(Long loanId, LoanRejectionReasonRequestDTO loanRejection) {
        Loan loan = new Loan();
        try {
            loan = loanService.rejectedLoan(loanId);
            Long idCustomer = getCustomerId();
            loan.setCustomerId(idCustomer);
            coreBankingClient.syncLoan(loanMapper.toDTO(loan));
            LoanRejectionReason rejectionReason = new LoanRejectionReason();
            rejectionReason.setReason(loanRejection.getReason());
            rejectionReason.setLoan(loanService.getLoanById(loanRejection.getLoan_id()).orElse(null));
            loanRejectionReasonService.save(rejectionReason);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return loan;
    }

    public List<Loan> findall() {
        return loanService.findAllLoan();
    }

    public void deleteLoan(Long loanId) {
        loanService.deleteLoan(loanId);
        coreBankingClient.deleteLoan(loanId);
    }

    public List<Repayment> getRepaymentsByLoanId(Long loanId) {
        return repaymentService.getRepaymentsByLoanId(loanId);
    }

    public String makeRepayment(Long repaymentId, BigDecimal amount, String accountNumber ) {
        PayRepaymentRequest pay = new PayRepaymentRequest();
        pay.setAmount(amount);
        pay.setCurrency("VND");
        pay.setDescription("Get transaction, sent otp");
        Repayment repayment = repaymentService.getRepaymentById(repaymentId).orElse(null);
        pay.setFromAccountNumber(accountNumber);
        CommonTransactionDTO transaction = commonTransactionService.loanPayment(pay);
        if (!transaction.getStatus().equalsIgnoreCase("PENDING")) {
            throw new IllegalArgumentException(transaction.getFailedReason());
        }
        return transaction.getReferenceCode();
    }

    public Repayment confirmRepayment(Long repaymentId, BigDecimal amount, String otpCode, String referenceCode) {
        CommonConfirmTransactionRequest confirm = new CommonConfirmTransactionRequest();
        confirm.setOtpCode(otpCode);
        confirm.setReferenceCode(referenceCode);
        CommonTransactionDTO transaction = commonTransactionService.confirmTransaction(confirm);
        if (!transaction.getStatus().equalsIgnoreCase("COMPLETED")) {
            throw new IllegalArgumentException(transaction.getFailedReason());
        }
        return repaymentService.makeRepayment(repaymentId, amount);
    }

    public List<Repayment> getHistory() {
        return repaymentService.getHistoryRepayment(getCustomerId());
    }

    public Repayment getCurrentRepayment() {
        Long idCustomer = getCustomerId();
        System.out.println(idCustomer);
        return repaymentService.getCurrentRepayment(idCustomer);
    }

//    public List<Repayment> getCurrentRepayments(Long loanId) {
//        List<Loan> loans = loanService.getLoansByCustomerId(
//                loanService.getLoanById(loanId).orElse(null).getCustomerId()
//        );
//        List<Repayment> repayments = new ArrayList<>();
//        for (Loan l : loans) {
//            repayments.add(repaymentService.getCurrentRepayment(l.getLoanId()));
//        }
//        return repayments;
//    }

    public Optional<Repayment> getRepaymentById(Long repaymentId) {
        return repaymentService.getRepaymentById(repaymentId);
    }

    public Repayment unpaidRepayment(Long repaymentId) {
        return repaymentService.updateRepaymentStatus(repaymentId, RepaymentStatus.UNPAID);
    }

    public Repayment lateRepayment(Long repaymentId) {
        return repaymentService.updateRepaymentStatus(repaymentId, RepaymentStatus.LATE);
    }

    public BigDecimal getTotalBorrowed() {
        Long idCustomer = getCustomerId();
        List<Loan> list = loanService.getLoansApproveAndCustomerId(idCustomer);
        return list.stream()
                .map(Loan::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalOutstanding() {
        Long idCustomer = getCustomerId();
        List<Loan> list = loanService.getLoansApproveAndCustomerId(idCustomer);
        BigDecimal totalOutstanding = BigDecimal.ZERO;
        for (Loan loan : list) {
            for (Repayment repayment : loan.getRepayments()) {
                BigDecimal x = repayment.getPrincipal()
                        .add(repayment.getInterest())
                        .subtract(repayment.getPaidAmount());
                totalOutstanding = totalOutstanding.add(x);
            }
        }
        return totalOutstanding;
    }


    public Long getCustomerId() {
//        JwtAuthenticationToken authentication = (JwtAuthenticationToken)
//                SecurityContextHolder.getContext().getAuthentication();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        System.out.println(auth);
        JwtAuthenticationToken jwt = (JwtAuthenticationToken) auth;
        String userId = jwt.getName();
//        String userId = authentication.getName();

        return commonService.getCurrentCustomer(userId).getCustomerId();
//        return 505L;
    }


    public void deleteRepaymentsByLoanId(Long loanId) {
        repaymentService.deleteRepaymentsByLoanId(loanId);
    }
}

