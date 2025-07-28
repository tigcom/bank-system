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
@Table(name = "terms")
public class Term {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "term_id")
    private Long termId;

    @Column(name = "term_value_months", nullable = false)
    private Integer termValueMonths; // Giá trị kỳ hạn theo tháng (ví dụ: 3, 6, 0 cho không kỳ hạn)

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 3)
    private BigDecimal interestRate; // Lãi suất, ví dụ: 4.500 (4.5%)

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true; // Trạng thái hoạt động của kỳ hạn

    @OneToMany(mappedBy = "term", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SavingsAccount> savingsAccounts;
}