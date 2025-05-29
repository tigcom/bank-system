package com.example.loan_service.handler;


import com.example.common_service.dto.AccountDTO;
import com.example.common_service.dto.CustomerResponseDTO;
import com.example.common_service.constant.CustomerStatus;
import com.example.common_service.models.KycStatus;
import com.example.common_service.services.account.AccountQueryService;
import com.example.common_service.services.customer.CustomerQueryService;
import com.example.common_service.services.customer.CustomerService;
import com.example.loan_service.entity.Loan;
import com.example.loan_service.entity.Repayment;
import com.example.loan_service.mapper.RepaymentMapper;
import com.example.loan_service.models.RepaymentStatus;
import com.example.loan_service.service.LoanService;
import com.example.loan_service.service.RepaymentService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.checkerframework.checker.units.qual.C;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LoanHandler {

    private final LoanService loanService;
    private final RepaymentService repaymentService;
    @DubboReference
    private final CustomerQueryService customerQueryService;

    @DubboReference
    private final AccountQueryService accountQueryService;

    public Loan createLoan(Loan loan) throws  Exception {
//        CustomerResponseDTO customer = CustomerResponseDTO.builder()
//                .id(12312L).cifCode("12312312").address("Hồ Chí Minh").email("khang@gmail.com")
//                .dateOfBirth(LocalDate.of(2003, 5, 10))
//                .status(CustomerStatus.ACTIVE).fullName("Phan Khang").kycStatus(KycStatus.VERIFIED)
//                .phoneNumber("1234567890").build();
//        AccountDTO account = AccountDTO.builder()
//                .accountNumber("123").cifCode("213122").accountType("PAYMENT")
//                .balance(BigDecimal.valueOf(1231232.00)).status("ACTIVE").build();
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
            return loanService.createLoan(loan);
        }
    }

    public Loan updateLoan(Loan loan) {
        return loanService.updateLoan(loan);
    }

    public Loan approveLoan(Long loanId) {
        Loan loan =  new Loan();
        try {
            loan = loanService.approveLoan(loanId);
            repaymentService.generateRepaymentSchedule(loan);
        }catch (Exception e) {
            e.printStackTrace();
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
        }catch (Exception e) {
            e.printStackTrace();
        }
        return loan;
    }

    public Loan rejectedLoan(Long loanId) {
        Loan loan =  new Loan();
        try {
            loan = loanService.rejectedLoan(loanId);
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
    }

    public List<Repayment> getRepaymentsByLoanId(Long loanId) {
        return repaymentService.getRepaymentsByLoanId(loanId);
    }

    public Repayment makeRepayment(Long repaymentId, java.math.BigDecimal amount) {


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
