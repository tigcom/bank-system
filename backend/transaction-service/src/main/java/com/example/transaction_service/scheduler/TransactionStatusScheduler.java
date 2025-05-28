package com.example.transaction_service.scheduler;

import com.example.transaction_service.entity.Transaction;
import com.example.transaction_service.enums.TransactionStatus;
import com.example.transaction_service.exception.ErrorCode;
import com.example.transaction_service.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TransactionStatusScheduler {
    private final TransactionRepository transactionRepository;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void checkPendingTransactions(){
        LocalDateTime timeoutTime = LocalDateTime.now().minusMinutes(3);

        List<Transaction> expiredTransactions = transactionRepository.
                findAllByStatusAndTimestampBefore(TransactionStatus.PENDING,timeoutTime);
        for(Transaction txn : expiredTransactions){
            txn.setStatus(TransactionStatus.FAILED);
            txn.setFailedReason(ErrorCode.TIME_OUT.getMessage());
        }
        transactionRepository.saveAll(expiredTransactions);
    }
}
