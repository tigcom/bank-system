package com.example.customer_service.repositories;

import com.example.customer_service.models.Customer;
import com.example.customer_service.models.KycProfile;
import com.example.customer_service.models.KycStatus;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface KycProfileRepository extends JpaRepository<KycProfile, Long> {
    Optional<KycProfile> findByCustomer(Customer customer);

    Page<KycProfile> findByStatus(KycStatus status, Pageable pageable);

    @Query("SELECT k FROM KycProfile k WHERE k.status = :status " +
            "AND (k.customer.cifCode LIKE %:keyword% " +
            "OR k.identityNumber LIKE %:keyword% " +
            "OR k.fullName LIKE %:keyword%)")
    Page<KycProfile> findByStatusAndKeyword(@Param("status") KycStatus status,
                                            @Param("keyword") String keyword,
                                            Pageable pageable);
}