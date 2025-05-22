package com.example.loan_service.service.impl;

import com.example.loan_service.entity.Loan;
import com.example.loan_service.entity.Repayment;
import com.example.loan_service.repository.LoanRepository;
import com.example.loan_service.service.LoanService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LoanServiceImpl implements LoanService {

    private final LoanRepository loanRepository;

    @Override
    public Loan createLoan(Loan loan) {
        loan.setStatus("PENDING");
        loan.setCreatedAt(LocalDateTime.now());
        return loanRepository.save(loan);
    }

    @Override
    public Loan updateLoan(Loan loan) {
        return loanRepository.save(loan);
    }

    @Override
    public List<Loan> findAllLoan() {
        return List.of();
    }

    @Override
    public Optional<Loan> getLoanById(Long loanId) {
        return loanRepository.findById(loanId);
    }

    @Override
    public Loan updateLoanStatus(Long loanId, String status) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new EntityNotFoundException("Loan not found"));
        loan.setStatus(status);
        return loanRepository.save(loan);
    }

    @Override
    public void deleteLoan(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new EntityNotFoundException("Loan not found"));

        if (!"PENDING".equalsIgnoreCase(loan.getStatus())) {
            throw new IllegalStateException("Only PENDING loans can be deleted");
        }

        loanRepository.deleteById(loanId);
    }

    @Override
    public Loan approveLoan(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new EntityNotFoundException("Loan not found"));

        if (!"PENDING".equalsIgnoreCase(loan.getStatus())) {
            throw new IllegalStateException("Loan is not in PENDING status");
        }

        loan.setStatus("APPROVED");
        loan.setApprovedAt(LocalDateTime.now());
        return loanRepository.save(loan);
    }

    @Override
    public List<Loan> getLoansByCustomerId(Long customerId) {
        return loanRepository.findAll().stream()
                .filter(loan -> loan.getCustomerId().equals(customerId))
                .toList();
    }

    @Override
    public Optional<Loan> getLoanByCustomerId(Long customerId,Long loanId) {
        return this.getLoansByCustomerId(customerId).stream()
                .filter(loan -> loan.getLoanId().equals(loanId))
                .findFirst();
    }

    @Override
    public List<Repayment> generateRepaymentSchedule(Loan loan) {
        List<Repayment> repayments = new ArrayList<>();

        BigDecimal monthlyInterestRate = loan.getInterestRate()
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);

        BigDecimal monthlyPrincipal = loan.getAmount()
                .divide(BigDecimal.valueOf(loan.getTermMonths()), 2, RoundingMode.HALF_UP);

        LocalDate dueDate = LocalDate.now().plusMonths(1);

        for (int i = 0; i < loan.getTermMonths(); i++) {
            BigDecimal interest = loan.getAmount().subtract(monthlyPrincipal.multiply(BigDecimal.valueOf(i)))
                    .multiply(monthlyInterestRate)
                    .setScale(2, RoundingMode.HALF_UP);

            Repayment repayment = new Repayment();
            repayment.setLoan(loan);
            repayment.setDueDate(dueDate.plusMonths(i));
            repayment.setPrincipal(monthlyPrincipal);
            repayment.setInterest(interest);
            repayment.setPaidAmount(BigDecimal.ZERO);
            repayment.setStatus("UNPAID");

            repayments.add(repayment);
        }

        return repayments;
    }


}
