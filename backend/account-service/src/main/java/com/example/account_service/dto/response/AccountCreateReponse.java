package com.example.account_service.dto.response;

import com.example.common_service.constant.AccountStatus;
import com.example.common_service.constant.AccountType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class AccountCreateReponse {
    private String id;
    private String accountNumber;
    private AccountType accountType;
    private String cifCode;
    private AccountStatus status;
}
