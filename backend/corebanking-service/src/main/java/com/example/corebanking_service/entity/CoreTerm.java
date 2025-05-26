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
@Table(name = "core_term_config")
public class CoreTerm {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "term_id")
    private Long termId;

    @Column(name = "term_value_months") // Giá trị kỳ hạn theo tháng (ví dụ: 3, 6, 0 cho không kỳ hạn)
    private Integer termValueMonths;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 3) // Lãi suất, ví dụ: 4.500 (4.5%)
    private BigDecimal interestRate;

    @Column(name = "is_active", nullable = false) // Trạng thái hoạt động của kỳ hạn
    private Boolean isActive;

    @OneToMany(mappedBy = "coreTerm")
    private List<CoreSavingsAccount> coreSavingsAccounts;

}
