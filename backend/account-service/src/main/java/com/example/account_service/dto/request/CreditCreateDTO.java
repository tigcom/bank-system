package com.example.account_service.dto.request;

import com.example.account_service.service.BaseAccountCreateDTO;
import com.example.common_service.constant.AccountStatus;
import com.example.common_service.constant.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditCreateDTO  implements BaseAccountCreateDTO  {
    private final com.example.common_service.constant.AccountType accountType = com.example.common_service.constant.AccountType.CREDIT;

    @NotBlank(message = "CIF code is required")
    private String cifCode;

    @NotNull(message = "Account status is required")
    private com.example.common_service.constant.AccountStatus status;


    @NotNull(message = "Credit limit is required")
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal creditLimit;

    @NotNull(message = "Interest rate is required")
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal interestRate;
}
