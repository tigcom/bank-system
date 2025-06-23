package com.example.account_service.repository;

import com.example.account_service.entity.Term;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.List;

@EnableJpaRepositories
public interface TermRepository extends JpaRepository<Term, Long> {
    
    Term findByTermValueMonths(Integer termValueMonths);
    
    @Query("SELECT t FROM Term t WHERE t.isActive = true")
    List<Term> findAllActiveTerms();
    
    @Query("SELECT t FROM Term t WHERE t.isActive = true ORDER BY t.termValueMonths")
    List<Term> findAllActiveTermsOrderByMonths();
}
