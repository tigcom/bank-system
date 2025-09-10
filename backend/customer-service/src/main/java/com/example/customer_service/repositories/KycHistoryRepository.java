package com.example.customer_service.repositories;

import com.example.customer_service.models.KycHistory;
import com.example.customer_service.models.KycStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface KycHistoryRepository extends JpaRepository<KycHistory, Long> {
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    long countByStatusAndCreatedAtBetween(KycStatus status, LocalDateTime start, LocalDateTime end);
}
