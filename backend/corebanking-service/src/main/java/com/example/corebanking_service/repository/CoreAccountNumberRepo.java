package com.example.corebanking_service.repository;

import com.example.common_service.dto.response.AccountSummaryDTO;
import com.example.common_service.dto.response.SavingAccountResponse;
import com.example.corebanking_service.entity.CoreAccount;
import com.example.corebanking_service.entity.CoreAccountNumber;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.query.Param;

import java.util.List;

@EnableJpaRepositories
public interface CoreAccountNumberRepo extends JpaRepository<CoreAccountNumber, Long> {

    @Query(value = "select * from core_account_numbers where status = 'available' limit 1",nativeQuery = true)
    CoreAccountNumber getAccountNumber();
    @Query(value = "select * from core_account_numbers where account_type_code = :typeCode  AND STATUS = 'AVAILABLE' limit 1;",nativeQuery = true)
    CoreAccountNumber getAccountNumberByTypeAccount(@Param("typeCode") String typeCode);
    boolean existsByNumber(String number);
    CoreAccountNumber findByNumber(String number);
}