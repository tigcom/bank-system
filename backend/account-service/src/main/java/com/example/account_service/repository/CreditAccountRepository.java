package com.example.account_service.repository;

import com.example.account_service.entity.CreditAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.query.Param;

import java.util.List;

@EnableJpaRepositories
public interface CreditAccountRepository extends JpaRepository<CreditAccount, String> {
    
    CreditAccount findByAccountNumber(String accountNumber);
    
    @Query("SELECT c FROM CreditAccount c WHERE c.cifCode = :cifCode AND c.status = 'ACTIVE'")
    List<CreditAccount> findActiveCreditAccountsByCifCode(@Param("cifCode") String cifCode);
    
    List<CreditAccount> findByCifCode(String cifCode);

    @Query("SELECT c FROM CreditAccount c WHERE c.cifCode = :cifCode")
    List<CreditAccount> findCreditAccountsByCifCode(String cifCode);
}