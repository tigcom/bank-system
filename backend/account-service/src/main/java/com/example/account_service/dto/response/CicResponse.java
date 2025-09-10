package com.example.account_service.dto.response;

import com.example.common_service.constant.AccountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CicResponse {
    private String status;
    private int creditScore;
    private boolean overdue;
    private int debtGroup;
} 