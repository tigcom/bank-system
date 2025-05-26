package com.example.account_service.dto.request;

import com.example.account_service.service.BaseAccountCreateDTO;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
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
public class SavingCreateDTO implements BaseAccountCreateDTO  {

    private final com.example.common_service.constant.AccountType accountType = com.example.common_service.constant.AccountType.SAVING;

    // nguon tien tu payment
    @NotBlank
    private String accountNumberSource;

    @NotNull(message = "Account status is required")
    private Long initialDeposit;

    @NotNull(message = "Term (in months) is required")
    @Min(1)
    private Integer term; // kỳ hạn (tháng)
}
