package com.example.loan_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public  class TransactionDto {
    private String id;
    private BigDecimal amount;
    private String timestamp;
    private String description;
    private String type;
}