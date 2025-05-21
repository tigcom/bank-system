package com.example.customer_service.repositories;

import com.example.customer_service.models.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByCifCode(String cifCode);
    Optional<Customer> findByIdentityNumber(String identityNumber);
    Optional<Customer> findByPhoneNumber(String phoneNumber);
    Optional<Customer> findByEmail(String email);
    List<Customer> findByFullNameContainingIgnoreCaseOrAddressContainingIgnoreCase(String name, String address);
}
