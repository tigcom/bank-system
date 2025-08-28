package com.example.loan_service.entity;
import  com.example.common_service.constant.LoanType;
import com.example.loan_service.models.LoanStatus;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "loan")
@Audited
@EntityListeners(AuditingEntityListener.class)
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "loan_id")
    private Long loanId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "disbursement_account_number",  length = 20)
    private String 	disbursementAccountNumber;
    @Column(name = "repayment_account_number", length = 20)
    private String 	repaymentAccountNumber;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "interest_rate", nullable = false)
    private BigDecimal interestRate;

    @Column(name = "term_months", nullable = false)
    private Integer termMonths;

    // bỏ field declaredIncome
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private LoanStatus status = LoanStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @CreatedDate
    @Column(name = "audit_created_at", updatable = false)
    private LocalDateTime auditCreatedAt;

    @LastModifiedDate
    @Column(name = "audit_last_modified_at")
    private LocalDateTime auditLastModifiedAt;

    @CreatedBy
    @Column(name = "audit_created_by", updatable = false)
    private String auditCreatedBy;

    @LastModifiedBy
    @Column(name = "audit_last_modified_by")
    private String auditLastModifiedBy;

    @OneToMany(mappedBy = "loan", fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<Repayment> repayments;

    @OneToMany(mappedBy = "loan", fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<LoanRejectionReason> rejectionReasons;

    @Column(name = "path_file")
    private String pathFile;;

    @Column(name = "declared_income")
    private BigDecimal declaredIncome;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private  LoanType loanType ;
}
 