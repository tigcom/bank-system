package com.example.loan_service.entity;


import com.example.loan_service.models.LoanStatus;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "loan")
@Data
@NoArgsConstructor
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "loan_id")
    private Long loanId;
    @Column(name = "customer_id", nullable = false)
    private Long customerId;
    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber;
    @Column(nullable = false)
    private BigDecimal amount;
    @Column(name = "interest_rate", nullable = false)
    private BigDecimal interestRate;
    @Column(name = "term_months", nullable = false)
    private Integer termMonths;
    @Column(name = "declared_income")
    private BigDecimal declaredIncome;
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private LoanStatus status = LoanStatus.PENDING;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    @OneToMany(mappedBy = "loan",fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Repayment> repayments;
}