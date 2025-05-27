package com.example.transaction_service.repository;

import com.example.transaction_service.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction,String> {
    Transaction findByReferenceCode(String referenceCode);
    @Query(value = "SELECT * FROM tbl_transaction " +
            "WHERE from_account_number = :accountNumber OR to_account_number = :accountNumber",
            nativeQuery = true)
    List<Transaction> getAccountTransactions(@Param("accountNumber") String accountNumber);
}
