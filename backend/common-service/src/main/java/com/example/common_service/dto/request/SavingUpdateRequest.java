package com.example.common_service.dto.request;

import com.example.common_service.constant.AccountStatus;
import com.example.common_service.constant.AccountType;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Builder
@Data
public class SavingUpdateRequest  implements Serializable {
    private static final long serialVersionUID = 1L;
    private BigDecimal balance;
    private AccountStatus status;
}
