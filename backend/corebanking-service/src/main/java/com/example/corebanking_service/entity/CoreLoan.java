package com.example.corebanking_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "core_loans")
public class CoreLoan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long loanId;

    @Column(name = "loan_amount")
    private Long loanAmount;

    @Column(name = "interest_rate")
    private Double interestRate;

    @Column(name = "term_months")
    private Integer termMonths;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "status", length = 20)
    private String status;

    @ManyToOne
    @JoinColumn(name = "account_number", nullable = false)
    private CoreAccount coreAccount;

}