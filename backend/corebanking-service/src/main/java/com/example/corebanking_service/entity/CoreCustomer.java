package com.example.corebanking_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "core_customers")
@Data
public class CoreCustomer {

    @Id
    @Column(name = "cif_code", nullable = false, length = 20)
    private String cifCode;

    @Column(name = "status", length = 20)
    private String status;

    @OneToMany(mappedBy = "coreCustomer")
    private List<CoreAccount> coreAccounts;
}
