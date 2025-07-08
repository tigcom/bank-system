package com.example.account_service.dto.response;

import com.example.common_service.constant.CreditRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreditSensitiveReponse {

    private String cardNumber;
    private String cardHolderName;
    private LocalDate cardExpiryDate;
}