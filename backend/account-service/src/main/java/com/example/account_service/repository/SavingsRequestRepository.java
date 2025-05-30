package com.example.account_service.repository;

import com.example.account_service.entity.CreditRequest;
import com.example.account_service.entity.SavingsRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories
public interface SavingsRequestRepository extends JpaRepository<SavingsRequest, String> {
}
