package com.example.loan_service.service.impl;

import com.example.loan_service.entity.Loan;
import com.example.loan_service.entity.Repayment;
import com.example.loan_service.models.LoanStatus;
import com.example.loan_service.models.RepaymentStatus;
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
        loan.setStatus(LoanStatus.PENDING);
        loan.setCreatedAt(LocalDateTime.now());
        return loanRepository.save(loan);
    }

    @Override
    public Loan updateLoan(Loan loan) {
        return loanRepository.save(loan);
    }

    @Override
    public List<Loan> findAllLoan() {
        return loanRepository.findAll();
    }

    @Override
    public List<Loan> getLoansApprove() {
        return loanRepository.findAllByStatusIs(LoanStatus.APPROVED);
    }

    @Override
    public Optional<Loan> getLoanById(Long loanId) {
        return loanRepository.findById(loanId);
    }

    @Override
    public void deleteLoan(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new EntityNotFoundException("Loan not found"));

        if (!LoanStatus.PENDING.equals(loan.getStatus())) {
            throw new IllegalStateException("Only PENDING loans can be deleted");
        }

        loanRepository.deleteById(loanId);
    }

    @Override
    public Loan approveLoan(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new EntityNotFoundException("Loan not found"));

        if (!LoanStatus.PENDING.equals(loan.getStatus())) {
            throw new IllegalStateException("Loan is not in PENDING status");
        }

        loan.setStatus(LoanStatus.APPROVED);
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
    public Loan rejectedLoan(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new EntityNotFoundException("Loan not found"));

        if (!LoanStatus.PENDING.equals(loan.getStatus())) {
            throw new IllegalStateException("Loan is not in PENDING status");
        }

        loan.setStatus(LoanStatus.REJECTED);
        loan.setApprovedAt(LocalDateTime.now());
        return loanRepository.save(loan);
    }

    @Override
    public Loan closedLoan(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new EntityNotFoundException("Loan not found"));

        if (!LoanStatus.APPROVED.equals(loan.getStatus())) {
            throw new IllegalStateException("Loan is not in APPROVED status");
        }
        loan.setStatus(LoanStatus.CLOSED);
        loan.setApprovedAt(LocalDateTime.now());
        return loanRepository.save(loan);
    }


}
