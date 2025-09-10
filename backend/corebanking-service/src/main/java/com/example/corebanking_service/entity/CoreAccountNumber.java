package com.example.corebanking_service.entity;

import com.example.common_service.constant.NumberStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "core_account_Numbers")
public class CoreAccountNumber {
    @Id
    @Column(name = "number", length = 10)
    private String number;
    @Column(name = "bank_code", length = 3, nullable = false)
    private String bankCode;
    @Column(name = "account_type_code", length =2, nullable = false)
    private String accountTypeCode;
    @Enumerated(EnumType.STRING)
    @Column(name="status",length = 10,nullable = false)
    private NumberStatus status;
    @Column(name = "generated_date", nullable = false)
    private LocalDateTime generatedDate;

}