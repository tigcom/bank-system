package com.example.corebanking_service.repository;

import com.example.common_service.dto.response.AccountSummaryDTO; // Bây giờ là một class
import com.example.corebanking_service.entity.CoreAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.query.Param; // Import @Param

import java.util.List;

@EnableJpaRepositories
public interface CoreAccountRepo extends JpaRepository<CoreAccount, String> {
    @Query(name = "AccountSummaryQueryResult", nativeQuery = true)
    List<AccountSummaryDTO> getAllAccountsByCif(@Param("cifCode") String cifCode);

    @Query("SELECT c FROM CoreAccount c WHERE c.coreCustomer.cifCode = :cifCode AND c.accountType = 'PAYMENT'")
    List<CoreAccount> getAllCorePaymentAccounts(@Param("cifCode") String cifCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM CoreAccount a WHERE a.accountNumber = :accountNumber")
    CoreAccount findByAccountNumberWithLock(@Param("accountNumber") String accountNumber);

    CoreAccount findByAccountNumber(String accountNumber);
}