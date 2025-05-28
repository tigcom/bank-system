package com.example.corebanking_service.entity;

import com.example.common_service.constant.AccountStatus;
import com.example.common_service.constant.AccountType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "core_accounts")
@SuperBuilder
@Inheritance(strategy = InheritanceType.JOINED)
public class CoreAccount {
    @Id
    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", length = 20)
    private AccountType accountType;

    @Column(name = "balance")
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private AccountStatus status;

    @Column(name = "opened_date")
    private LocalDate openedDate;

    @ManyToOne
    @JoinColumn(name = "cif_code", nullable = false)
    private CoreCustomer coreCustomer;

    @OneToMany(mappedBy = "coreAccount")
    private List<CoreLoan> loans;

    @OneToMany(mappedBy = "fromAccount")
    private List<CoreTransaction> outgoingTransactions;

    @OneToMany(mappedBy = "toAccount")
    private List<CoreTransaction> incomingTransactions;
}
