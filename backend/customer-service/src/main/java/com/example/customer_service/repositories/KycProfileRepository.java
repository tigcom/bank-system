package com.example.customer_service.repositories;

import com.example.customer_service.models.Customer;
import com.example.customer_service.models.KycProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KycProfileRepository extends JpaRepository<KycProfile, Long> {
    Optional<KycProfile> findByCustomer(Customer customer);
}

