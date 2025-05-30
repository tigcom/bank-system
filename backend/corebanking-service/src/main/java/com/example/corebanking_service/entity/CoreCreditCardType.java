package com.example.corebanking_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "core_creditcard_type")
public class CoreCreditCardType {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "cart_type_id")
    private String id;

    @Column(name = "type_name", nullable = false, unique = true)
    private String typeName;

    @Column(name = "default_credit_limit", nullable = false)
    private BigDecimal defaultCreditLimit;

    @Column(name = "interest_rate", nullable = false)
    private BigDecimal interestRate;

    @Column(name = "annual_fee")
    private BigDecimal annualFee;

    @Column(name="minimum_Income")
    private BigDecimal minimumIncome;

    @Column(name="image_Url")
    private String imageUrl;

    @Column(name = "description")
    private String description;

    @ElementCollection
    @CollectionTable(name = "credit_card_type_conditions", joinColumns = @JoinColumn(name = "cart_type_id"))
    @Column(name = "condition_text")
    private List<String> conditions;

    @OneToMany(mappedBy = "coreCreditCardType")
    private List<CoreCreditAccount> coreCreditAccounts;
}

