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

import java.math.BigDecimal;
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
            "      AND type != 'EXTERNAL_TRANSFER'\n" +
            "    GROUP BY to_account_number\n" +
            "    ORDER BY MAX(created_at) DESC\n" +
            "    LIMIT 5\n" +
            ") latest_tx\n" +
            "ON t.id = latest_tx.latest_id\n" +
            "ORDER BY t.created_at DESC;\n",
            nativeQuery = true)
    List<String> getListToAccountNumberLatest(String fromAccountNumber);

    @Query(value = "SELECT * FROM tbl_transaction t\n" +
            "WHERE t.type = 'PAY_BILL' \n" +
            "    AND t.status = 'COMPLETED'\n" +
            "    AND t.timestamp >= :startOfDay AND t.timestamp < :endOfDay",
            nativeQuery = true)
    List<Transaction> getDailyPaymentTransaction(@Param("startOfDay") LocalDateTime startOfDay,
                                                 @Param("endOfDay") LocalDateTime endOfDay);

    @Query(value = "SELECT * FROM tbl_transaction " +
            "WHERE (from_account_number = :accountNumber OR to_account_number = :accountNumber) " +
            "ORDER BY timestamp DESC",
            nativeQuery = true)
    Page<Transaction> findByAccountNumber(@Param("accountNumber") String accountNumber, Pageable pageable);


    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.createdAt BETWEEN :start AND :end")
    BigDecimal sumAmountByCreatedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    long countByStatusAndCreatedAtBetween(TransactionStatus status, LocalDateTime start, LocalDateTime end);

    @Query("SELECT t.type, COUNT(t), SUM(t.amount) FROM Transaction t " +
            "WHERE t.createdAt BETWEEN :start AND :end GROUP BY t.type")
    List<Object[]> groupByTypeAndSum(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT t.fromAccountNumber, COUNT(t), SUM(t.amount) " +
            "FROM Transaction t " +
            "WHERE t.createdAt BETWEEN :start AND :end " +
            "GROUP BY t.fromAccountNumber " +
            "ORDER BY SUM(t.amount) DESC")
    List<Object[]> findTopAccounts(@Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end,
                                   Pageable pageable);

    List<Transaction> findTop5ByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end);

}