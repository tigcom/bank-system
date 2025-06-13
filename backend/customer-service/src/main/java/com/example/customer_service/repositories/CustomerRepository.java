package com.example.customer_service.repositories;

import com.example.customer_service.models.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByUserId(String userId);

    Customer findCustomerByUserId(String userId);

    Optional<Customer> findByResetToken(String resetToken);

    Optional<Customer> findByCifCode(String cifCode);

    Optional<Customer> findByIdentityNumber(String identityNumber);

    Optional<Customer> findByPhoneNumber(String phoneNumber);

    Optional<Customer> findByUsername(String username);

    Optional<Customer> findByEmail(String email);

    @Query("SELECT COALESCE(MAX(c.customerId), 0) + 1 FROM Customer c")
    Long getNextId();

    @Query("SELECT c FROM Customer c WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR " +
            "LOWER(c.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.identityNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.cifCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.phoneNumber) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Customer> searchCustomers(@Param("keyword") String keyword, Pageable pageable);


}
