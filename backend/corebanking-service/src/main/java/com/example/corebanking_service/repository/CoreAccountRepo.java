package com.example.corebanking_service.repository;

import com.example.common_service.dto.response.AccountSummaryDTO; // Bây giờ là một class
import com.example.corebanking_service.entity.CoreAccount;
import org.springframework.data.jpa.repository.JpaRepository;
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


    CoreAccount findByAccountNumber(String accountNumber);
}