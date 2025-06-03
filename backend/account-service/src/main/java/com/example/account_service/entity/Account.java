package com.example.account_service.entity;

import com.example.common_service.constant.AccountStatus;
import com.example.common_service.constant.AccountType;
import jakarta.persistence.*;
import jdk.jfr.DataAmount;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name="accounts")
@Builder
public class Account  extends  Auditable implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "account_number", nullable = false, unique = true, length = 20)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", length = 20)
    private AccountType accountType;

    @Column(name = "cif_code", nullable = false, length = 20)
    private String cifCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private AccountStatus status;

}
