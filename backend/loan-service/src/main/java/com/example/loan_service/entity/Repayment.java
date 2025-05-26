package com.example.loan_service.entity;

import com.example.loan_service.models.RepaymentStatus;
import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "repayment")
@Data
@NoArgsConstructor
public class Repayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "repayment_id")
    private Long repaymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    @JsonBackReference
    private Loan loan;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false)
    private BigDecimal principal;

    @Column(nullable = false)
    private BigDecimal interest;

    @Column(name = "paid_amount", nullable = false)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private RepaymentStatus status = RepaymentStatus.UNPAID;
}
