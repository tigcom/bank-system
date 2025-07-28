package com.example.account_service.dto.request;

import com.example.common_service.constant.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSimpleAccountRequest {
    private String accountNumber;
    private AccountStatus status;
} 