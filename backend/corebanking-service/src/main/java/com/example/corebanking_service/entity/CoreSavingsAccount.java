package com.example.corebanking_service.entity;

import com.example.corebanking_service.constant.AccountType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@Table(name = "savings_account")
public class CoreSavingsAccount  extends CoreAccount{
    @Column(name = "initial_deposit", nullable = false)
    private Long initialDeposit;

    @Column(name = "term_months", nullable = false)
    private Integer termMonths;

    @Column(name = "interest_rate")
    private Long interestRate;

    @Column(name = "maturity_date")
    private LocalDate maturityDate;

    public  CoreSavingsAccount() {
        super();
        this.setAccountType(AccountType.SAVINGS);
    }

}
