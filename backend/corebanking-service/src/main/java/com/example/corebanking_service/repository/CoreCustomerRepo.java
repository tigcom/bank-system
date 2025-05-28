package com.example.corebanking_service.repository;

import com.example.corebanking_service.entity.CoreAccount;
import com.example.corebanking_service.entity.CoreCustomer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories
public interface CoreCustomerRepo extends  JpaRepository<CoreCustomer,String>{
    CoreCustomer getCoreCustomerByCifCode(String cifCode);
}
