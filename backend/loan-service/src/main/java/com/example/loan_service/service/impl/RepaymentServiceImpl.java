package com.example.loan_service.service.impl;

import com.example.loan_service.entity.Loan;
import com.example.loan_service.entity.Repayment;
import com.example.loan_service.repository.RepaymentRepository;
import com.example.loan_service.service.RepaymentService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RepaymentServiceImpl implements RepaymentService {

    private final RepaymentRepository repaymentRepository;

    @Override
    public List<Repayment> generateRepaymentSchedule(Loan loan) {
        List<Repayment> repayments = new ArrayList<>();

        BigDecimal monthlyInterestRate = loan.getInterestRate()
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);

        BigDecimal monthlyPrincipal = loan.getAmount()
                .divide(BigDecimal.valueOf(loan.getTermMonths()), 2, RoundingMode.HALF_UP);

        LocalDate startDueDate = LocalDate.now().plusMonths(1);

        for (int i = 0; i < loan.getTermMonths(); i++) {
            Repayment repayment = calculateRepayment(loan, i, monthlyPrincipal, monthlyInterestRate, startDueDate);
            repayments.add(repayment);
        }

        return repayments;
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
            updatedRepayments.add(repayment);
        }

        return updatedRepayments;
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
    public Repayment updateRepaymentStatus(Long repaymentId, String status) {
        Repayment repayment = repaymentRepository.findById(repaymentId)
                .orElseThrow(() -> new EntityNotFoundException("Repayment not found"));
        repayment.setStatus(status);
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
            repayment.setStatus("PAID");
        } else if (newPaidAmount.compareTo(BigDecimal.ZERO) > 0) {
            repayment.setStatus("PARTIAL");
        }

        return repaymentRepository.save(repayment);
    }

    @Override
    public void deleteRepaymentsByLoanId(Long loanId) {
        List<Repayment> repayments = getRepaymentsByLoanId(loanId);
        repaymentRepository.deleteAll(repayments);
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
        repayment.setStatus("UNPAID");

        return repayment;
    }

}
