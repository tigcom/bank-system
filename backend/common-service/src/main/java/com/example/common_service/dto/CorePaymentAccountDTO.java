package com.example.common_service.dto;

import com.example.common_service.constant.AccountStatus;
import com.example.common_service.constant.AccountType;
import com.example.common_service.services.CoreAccountBaseDTO;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;


@Data
@Builder
public class CorePaymentAccountDTO  implements  Serializable {
    private String cifCode;
    private String accountNumber;
    private Long balance;
    private final AccountStatus status = AccountStatus.ACTIVE;
    private final AccountType accountType = AccountType.PAYMENT;

}
