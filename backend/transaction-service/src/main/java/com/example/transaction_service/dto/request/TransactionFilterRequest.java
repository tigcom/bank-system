package com.example.transaction_service.dto.request;

import com.example.transaction_service.enums.BankType;
import com.example.transaction_service.enums.CurrencyType;
import com.example.transaction_service.enums.TransactionStatus;
import com.example.transaction_service.enums.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionFilterRequest {
    private String accountNumber;
    private String keyword;
    private TransactionType type;
    private TransactionStatus status;
    private CurrencyType currency;
    private BankType bankType;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String sortBy;
    private String sortDirection;
    private BigDecimal fromAmount;
    private BigDecimal toAmount;
    private int page;
    private int size;
}
