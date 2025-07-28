package com.example.account_service.repository;

import com.example.account_service.entity.LoanAccount;
import com.example.common_service.constant.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LoanAccountRepository extends JpaRepository<LoanAccount, String> {

    LoanAccount findByAccountNumber(String accountNumber);

    List<LoanAccount> findByCifCode(String cifCode);

    List<LoanAccount> findByCifCodeAndStatus(String cifCode, AccountStatus status);

    @Query("SELECT l FROM LoanAccount l WHERE l.cifCode = :cifCode AND l.status = 'ACTIVE'")
    List<LoanAccount> findActiveLoanAccountsByCifCode(@Param("cifCode") String cifCode);

    boolean existsByAccountNumber(String accountNumber);

    boolean existsByAccountNumberAndCifCode(String accountNumber, String cifCode);
}
