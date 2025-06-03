package com.example.account_service.repository;

import com.example.account_service.entity.Account;
import com.example.common_service.dto.AccountDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.List;

@EnableJpaRepositories
public interface AccountRepository extends JpaRepository<Account, String> {
    Account findByAccountNumber(String accountNumber);

    List<Account> findByCifCode(String cifCode);
}
