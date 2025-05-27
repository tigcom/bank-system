package com.example.account_service.entity;

import com.example.common_service.constant.CreditRequestStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@Table(name = "credit_request")
public class CreditRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // Mã định danh khách hàng (liên kết với hệ thống corebanking)
    @Column(name = "cif_code", nullable = false)
    private String cifCode;

    @Column(name = "occupation")
    private String occupation;

    @Column(name = "monthly_income")
    private BigDecimal monthlyIncome;

    @Column(name = "card_type_id")
    private String cartTypeId; // ex: "VISA", "MASTER", etc.

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private CreditRequestStatus status; // PENDING, APPROVED, REJECTED

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = CreditRequestStatus.PENDING;
        }
    }
}
