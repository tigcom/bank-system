package com.example.account_service.repository;

import com.example.account_service.entity.CreditCardType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.List;
import java.util.Optional;

@EnableJpaRepositories
public interface CreditCardTypeRepository extends JpaRepository<CreditCardType, String> {
    
    Optional<CreditCardType> findByTypeName(String typeName);
    
    @Query("SELECT c FROM CreditCardType c ORDER BY c.annualFee ASC")
    List<CreditCardType> findAllOrderByAnnualFee();
}
