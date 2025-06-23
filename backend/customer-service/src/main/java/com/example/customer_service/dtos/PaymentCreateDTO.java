package com.example.customer_service.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCreateDTO  implements Serializable {
    private static final long serialVersionUID = 1L;
    private final com.example.common_service.constant.AccountType accountType = com.example.common_service.constant.AccountType.PAYMENT;

    @NotBlank(message = "CIF code is required")
    private String cifCode;

}
