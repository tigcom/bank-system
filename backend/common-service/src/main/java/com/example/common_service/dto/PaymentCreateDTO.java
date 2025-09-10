package com.example.common_service.dto;

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

    private String cifCode;

}
