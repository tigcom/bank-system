package com.example.account_service.dto.response;

import com.example.common_service.constant.CreditRequestStatus;
import com.example.common_service.constant.SavingsRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SavingsRequestResponse {

    private String id;
    private String cifCode;
    private BigDecimal initialDeposit;
    private Integer term;
    private String accountNumberSource;
    private BigDecimal interestRate;
    private SavingsRequestStatus status; // PENDING, APPROVED, FAILED

}
