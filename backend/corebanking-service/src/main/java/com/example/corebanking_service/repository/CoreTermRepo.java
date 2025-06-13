package com.example.corebanking_service.repository;

import com.example.corebanking_service.entity.CoreAccount;
import com.example.corebanking_service.entity.CoreTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.List;

@EnableJpaRepositories
public interface CoreTermRepo extends  JpaRepository<CoreTerm,Long>{
    CoreTerm getCoreTermsByTermValueMonths(Integer termValueMonths);

    @Query("SELECT c FROM CoreTerm  c where  c.isActive= true")
    List<CoreTerm> getAllCoreTermActive();
}
