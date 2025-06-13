package com.example.corebanking_service.entity;

import com.example.common_service.constant.AccountType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@SuperBuilder
@Table(name = "savings_account")
public class CoreSavingsAccount  extends CoreAccount{
    @Column(name = "initial_deposit", nullable = false)
    private BigDecimal initialDeposit;

    @Column(name="src_account-number")
    private String srcAccountNumber;
    @ManyToOne
    @JoinColumn(name = "term_id", nullable = false)
    private CoreTerm coreTerm;

    public  CoreSavingsAccount() {
        super();
        this.setAccountType(AccountType.SAVING);
    }

}
