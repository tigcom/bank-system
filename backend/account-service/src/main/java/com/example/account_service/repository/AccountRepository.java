package com.example.account_service.repository;

import com.example.account_service.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories
public interface AccountRepository extends JpaRepository<Account, String> {
}
