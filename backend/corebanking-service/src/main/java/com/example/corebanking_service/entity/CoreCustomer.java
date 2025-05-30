package com.example.corebanking_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "core_customers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class CoreCustomer {

    @Id
    @Column(name = "cif_code", nullable = false, length = 20)
    private String cifCode;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Builder.Default
    @OneToMany(mappedBy = "coreCustomer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CoreAccount> coreAccounts = new ArrayList<>();
}