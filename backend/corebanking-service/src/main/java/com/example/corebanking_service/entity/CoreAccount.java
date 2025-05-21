package com.example.corebanking_service.entity;

import com.example.corebanking_service.constant.AccountType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "core_accounts")
@Inheritance(strategy = InheritanceType.JOINED)
public class CoreAccount {
    @Id
    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", length = 20)
    private AccountType accountType;

    @Column(name = "balance")
    private Long balance;

    @Column(name = "status", length = 20)
    private String status;

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
