package com.example.account_service.dto.request;

import com.example.common_service.constant.AccountStatus;
import com.example.common_service.constant.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Builder
public class PaymentCreateDTO  {

    private final com.example.common_service.constant.AccountType accountType = com.example.common_service.constant.AccountType.PAYMENT;

}
