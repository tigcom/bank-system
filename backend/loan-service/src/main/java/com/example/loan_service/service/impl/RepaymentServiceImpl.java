package com.example.loan_service.service.impl;

import com.example.loan_service.entity.Loan;
import com.example.loan_service.entity.Repayment;
import com.example.loan_service.mapper.LoanMapper;
import com.example.loan_service.models.RepaymentStatus;
import com.example.loan_service.repository.RepaymentRepository;
import com.example.loan_service.service.CoreBankingClient;
import com.example.loan_service.service.LoanService;
import com.example.loan_service.service.RepaymentService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RepaymentServiceImpl implements RepaymentService {

    private final RepaymentRepository repaymentRepository;
    private final LoanService loanService;
    private final LoanMapper loanMapper;
    private final CoreBankingClient bankingClient;
    @Override
    public List<Repayment> getRepaymentNotPaid(Long loanId) {
        return repaymentRepository.findUnpaidByLoanIdOrderByDueDate(loanId);
    }

    @Override
    public List<Repayment> generateRepaymentSchedule(Loan loan) {
        List<Repayment> repayments = new ArrayList<>();
        // lai xuat thang = tong lai 1 nam / 12 thang
        BigDecimal monthlyInterestRate = loan.getInterestRate()
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
        // tinh so tien goc phai tra moi thang = tong so tien / so thang vay
        BigDecimal monthlyPrincipal = loan.getAmount()
                .divide(BigDecimal.valueOf(loan.getTermMonths()), 2, RoundingMode.HALF_UP);
        // thoi gian bat dau tra no = thoi gian hien tai + 1 thang
        LocalDate startDueDate = LocalDate.now().plusMonths(1);

        for (int i = 0; i < loan.getTermMonths(); i++) {
        // tinh so tien no con lai
            BigDecimal remainingPrincipal = loan.getAmount().subtract(monthlyPrincipal.multiply(BigDecimal.valueOf(i)));
            Repayment repayment = new Repayment();
            repayment.setLoan(loan);
            repayment.setDueDate(startDueDate.plusMonths(i));
            repayment.setPrincipal(monthlyPrincipal);
        // tinh tien lai theo tong no giam dan
            repayment.setInterest(monthlyInterestRate.multiply(remainingPrincipal).setScale(2, RoundingMode.HALF_UP));
            repayment.setPaidAmount(BigDecimal.ZERO);
            repayment.setStatus(RepaymentStatus.UNPAID);
            repaymentRepository.save(repayment);
            repayments.add(repayment);
        }

        return repayments;
    }

    @Override
    public List<Repayment> getRepaymentsByLoanId(Long loanId) {
        return repaymentRepository.findAll().stream()
                .filter(repayment -> repayment.getLoan().getLoanId().equals(loanId))
                .toList();
    }

    @Override
    public Optional<Repayment> getRepaymentById(Long repaymentId) {
        return repaymentRepository.findById(repaymentId);
    }

    @Override
    public List<Repayment> getHistoryRepayment(Long loanId) {
        return repaymentRepository.findPaidOrPartialByLoanId(loanId);
    }

    @Override
    public Repayment updateRepaymentStatus(Long repaymentId, RepaymentStatus status) {
        Repayment repayment = repaymentRepository.findById(repaymentId)
                .orElseThrow(() -> new EntityNotFoundException("Repayment not found"));
        repayment.setStatus(status);
        return repaymentRepository.save(repayment);
    }

    @Override
    public Repayment updateRepayment(Repayment repayment) {
        return repaymentRepository.save(repayment);
    }


    @Override
    public Repayment makeRepayment(Long repaymentId, BigDecimal amount) {
        Repayment repayment = repaymentRepository.findById(repaymentId)
                .orElseThrow(() -> new EntityNotFoundException("Repayment not found"));

        BigDecimal newPaidAmount = repayment.getPaidAmount().add(amount);
        repayment.setPaidAmount(newPaidAmount);

        BigDecimal totalDue = repayment.getPrincipal().add(repayment.getInterest());

        if (newPaidAmount.compareTo(totalDue) >= 0) {
            repayment.setStatus(RepaymentStatus.PAID);
            if(checkLastMonthRepayment(repayment)) {
                loanService.closedLoan(repayment.getLoan().getLoanId());
                bankingClient.syncLoan(loanMapper.toRequestDTO(repayment.getLoan()));
            }
        } else if (newPaidAmount.compareTo(BigDecimal.ZERO) > 0) {
            repayment.setStatus(RepaymentStatus.PARTIAL);
        }
        return repaymentRepository.save(repayment);
    }

    @Override
    public void deleteRepaymentsByLoanId(Long loanId) {
        List<Repayment> repayments = getRepaymentsByLoanId(loanId);
        repaymentRepository.deleteAll(repayments);
    }

    @Override
    public Repayment getCurrentRepayment(Long loanId) {
        return repaymentRepository.findNextRepaymentNative(loanId);
    }

    private Repayment calculateRepayment(Loan loan, int periodIndex, BigDecimal monthlyPrincipal, BigDecimal monthlyInterestRate, LocalDate startDueDate) {
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

        return repayment;
    }

    public List<Repayment> updateRepaymentSchedule(Loan loan, int startPeriodIndex, BigDecimal remainingPrincipal) {
        List<Repayment> updatedRepayments = new ArrayList<>();

        BigDecimal monthlyInterestRate = loan.getInterestRate()
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);

        int remainingTerm = loan.getTermMonths() - startPeriodIndex;

        BigDecimal updatedMonthlyPrincipal = remainingPrincipal
                .divide(BigDecimal.valueOf(remainingTerm), 2, RoundingMode.HALF_UP);

        LocalDate startDueDate = LocalDate.now().plusMonths(1);

        for (int i = 0; i < remainingTerm; i++) {
            Repayment repayment = calculateRepayment(
                    loan,
                    i + startPeriodIndex,
                    updatedMonthlyPrincipal,
                    monthlyInterestRate,
                    startDueDate
            );
            repaymentRepository.save(repayment);
            updatedRepayments.add(repayment);
        }

        return updatedRepayments;
    }

    public Boolean checkLastMonthRepayment(Repayment repayment) {
        List<Repayment> repayments = getRepaymentsByLoanId(repayment.getLoan().getLoanId());
        Boolean check = false;
        if (repayments.size() == 1) {
            return true;
        }else{
            LocalDate dueDate = repayment.getDueDate();
            System.out.println("=======================");
            for (Repayment r : repayments) {
                System.out.println("check:"+dueDate+"---"+r.getDueDate()+"kq"+dueDate.compareTo(r.getDueDate()));
                if(dueDate.compareTo(r.getDueDate()) >= 0){
                    check = true;
                }else{
                    check = false;
                }
            }
        }
        return check;
    }
}
