package com.example.account_service.entity;

import com.example.common_service.constant.AccountType;
import com.example.common_service.constant.InterestPaymentType;
import com.example.common_service.constant.RenewOption;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(name = "savings_accounts")
public class SavingsAccount extends Account {

    @Column(name = "initial_deposit", nullable = false, precision = 19, scale = 2)
    private BigDecimal initialDeposit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id", nullable = false)
    private Term term;

    @Column(name = "maturity_date", nullable = false)
    private LocalDateTime maturityDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "interest_payment_type", nullable = false)
    private InterestPaymentType interestPaymentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "renew_option", nullable = false)
    private RenewOption renewOption;

    @Column(name="accountnumber_src")
    private String accountNumberSrc;

    @Override
    public AccountType getAccountType() {
        return AccountType.SAVING;
    }
}