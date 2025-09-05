package com.example.loan_service.entity;

import com.example.loan_service.models.RepaymentStatus;
import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "repayment")
@Data
@NoArgsConstructor
@Audited
@EntityListeners(AuditingEntityListener.class)
public class Repayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "repayment_id")
    private Long repaymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    @JsonBackReference
    private Loan loan;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false)
    private BigDecimal principal;

    @Column(nullable = false)
    private BigDecimal interest;

    @Column(name = "paid_amount", nullable = false)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private RepaymentStatus status = RepaymentStatus.UNPAID;

    @CreatedDate
    @Column(name = "audit_created_at", updatable = false)
    private java.time.LocalDateTime auditCreatedAt;

    @LastModifiedDate
    @Column(name = "audit_last_modified_at")
    private java.time.LocalDateTime auditLastModifiedAt;

    @CreatedBy
    @Column(name = "audit_created_by", updatable = false)
    private String auditCreatedBy;

    @LastModifiedBy
    @Column(name = "audit_last_modified_by")
    private String auditLastModifiedBy;
}
