package com.example.account_service.entity;

import com.example.common_service.constant.CreditRequestStatus;
import com.example.common_service.constant.SavingsRequestStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Builder
@Table(name = "savings_request")
public class SavingsRequest extends  Auditable implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Column(name = "cif_code", nullable = false)
    private String cifCode;

    @Column(name="initialDeposit")
    private BigDecimal initialDeposit;

    @Column(name="term")
    private Integer term;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 3) // Lãi suất, ví dụ: 4.500 (4.5%)
    private BigDecimal interestRate;


    @Column(name="accountNumberSource")
    private String accountNumberSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SavingsRequestStatus status; // PENDING, APPROVED, FAILED

//    @Column(name = "created_at")
//    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.status == null) {
            this.status = SavingsRequestStatus.PENDING;
        }
    }
}
