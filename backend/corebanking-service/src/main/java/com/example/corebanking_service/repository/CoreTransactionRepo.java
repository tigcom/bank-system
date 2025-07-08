package com.example.corebanking_service.repository;

import com.example.corebanking_service.entity.CoreTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CoreTransactionRepo extends  JpaRepository<CoreTransaction,Long>{


}
