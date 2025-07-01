package com.example.customer_service.repositories;

import com.example.customer_service.models.Customer;
import com.example.customer_service.models.KycProfile;
import com.example.customer_service.models.KycStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface KycProfileRepository extends JpaRepository<KycProfile, Long> {
    Optional<KycProfile> findByCustomer(Customer customer);

    Page<KycProfile> findByStatus(KycStatus status, Pageable pageable);

    long countByStatusAndCreatedAtBetween(KycStatus status, LocalDateTime start, LocalDateTime end);
}