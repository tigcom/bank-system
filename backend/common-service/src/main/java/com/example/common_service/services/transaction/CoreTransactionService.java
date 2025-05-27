package com.example.common_service.services.transaction;

import com.example.common_service.dto.TransactionRequest;

public interface CoreTransactionService {
    void performTransfer(TransactionRequest request);
}
