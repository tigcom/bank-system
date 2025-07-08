package com.example.account_service.repository;

import com.example.account_service.entity.SavingsAccount;
import com.example.common_service.constant.AccountStatus;
import com.example.common_service.constant.InterestPaymentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@EnableJpaRepositories
public interface SavingsAccountRepository extends JpaRepository<SavingsAccount, String> {
    
    SavingsAccount findByAccountNumber(String accountNumber);
    
    @Query("SELECT s FROM SavingsAccount s WHERE s.cifCode = :cifCode AND s.status = 'ACTIVE'")
    List<SavingsAccount> findActiveSavingsAccountsByCifCode(@Param("cifCode") String cifCode);

    List<SavingsAccount> findByInterestPaymentTypeAndStatus(InterestPaymentType interestPaymentType, AccountStatus status);

    List<SavingsAccount> findByCifCode(String cifCode);

    List<SavingsAccount> findByMaturityDateAndStatus(LocalDateTime today, AccountStatus status);
}