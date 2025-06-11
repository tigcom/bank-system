package com.example.transaction_service.repository;

import com.example.transaction_service.entity.Transaction;
import com.example.transaction_service.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction,String>, JpaSpecificationExecutor<Transaction> {
    Transaction findByReferenceCode(String referenceCode);
    @Query(value = "SELECT * FROM tbl_transaction " +
            "WHERE from_account_number = :accountNumber OR to_account_number = :accountNumber",
            nativeQuery = true)
    List<Transaction> getAccountTransactions(@Param("accountNumber") String accountNumber);

    List<Transaction> findAllByStatusAndTimestampBefore(TransactionStatus status, LocalDateTime beforeTime);

    @Query(value = "SELECT t.to_account_number\n" +
            "FROM tbl_transaction t\n" +
            "JOIN (\n" +
            "    SELECT MAX(id) AS latest_id\n" +
            "    FROM tbl_transaction\n" +
            "    WHERE from_account_number = :fromAccountNumber\n" +
            "    GROUP BY to_account_number\n" +
            "    ORDER BY MAX(created_at) DESC\n" +
            "    LIMIT 5\n" +
            ") latest_tx\n" +
            "ON t.id = latest_tx.latest_id\n" +
            "ORDER BY t.created_at DESC;\n",
    nativeQuery = true)
    List<String> getListToAccountNumberLatest(String fromAccountNumber);



    
}
