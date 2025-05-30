package com.example.account_service.repository;

import com.example.account_service.entity.Account;
import com.example.account_service.entity.CreditRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories
public interface CreditRequestRepository extends JpaRepository<CreditRequest, String> {
}
