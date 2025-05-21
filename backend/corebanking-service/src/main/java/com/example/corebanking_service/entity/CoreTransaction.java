package com.example.corebanking_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "core_transactions")
public class CoreTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;

    @Column(name = "amount")
    private Long amount;

    @Column(name = "transaction_type", length = 20)
    private String transactionType;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @ManyToOne
    @JoinColumn(name = "from_account")
    private CoreAccount fromAccount;

    @ManyToOne
    @JoinColumn(name = "to_account")
    private CoreAccount toAccount;

}