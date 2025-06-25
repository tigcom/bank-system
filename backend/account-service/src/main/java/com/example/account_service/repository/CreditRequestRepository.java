package com.example.account_service.repository;

import com.example.account_service.entity.Account;
import com.example.account_service.entity.CreditRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.List;

@EnableJpaRepositories
public interface CreditRequestRepository extends JpaRepository<CreditRequest, String> {

    @Query("SELECT c FROM CreditRequest c WHERE  c.status != 'APPROVED' AND c.status!='REJECTED' ")
    List<CreditRequest> findAllByStatus();
}
