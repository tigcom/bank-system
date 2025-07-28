package com.example.account_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "credit_card_types")
public class CreditCardType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "card_type_id")
    private String id;

    @Column(name = "type_name", nullable = false, unique = true)
    private String typeName;

    @Column(name = "default_credit_limit", nullable = false, precision = 19, scale = 2)
    private BigDecimal defaultCreditLimit;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 3)
    private BigDecimal interestRate;

    @Column(name = "annual_fee", precision = 19, scale = 2)
    private BigDecimal annualFee;

    @Column(name = "minimum_income", precision = 19, scale = 2)
    private BigDecimal minimumIncome;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "description")
    private String description;

    @Column(name = "type_Card")
    private String cardType;


//    @ElementCollection
//    @CollectionTable(name = "credit_card_type_conditions", joinColumns = @JoinColumn(name = "card_type_id"))
//    @Column(name = "condition_text")
//    private List<String> conditions;
    @OneToMany(mappedBy = "creditCardType", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CreditAccount> creditAccounts;
}
