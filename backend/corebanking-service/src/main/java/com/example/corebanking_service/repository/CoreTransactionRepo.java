package com.example.corebanking_service.repository;

import com.example.corebanking_service.entity.CoreAccount;
import com.example.corebanking_service.entity.CoreTransaction;
import org.apache.dubbo.remoting.http12.rest.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoreTransactionRepo extends  JpaRepository<CoreTransaction,Long>{


}
