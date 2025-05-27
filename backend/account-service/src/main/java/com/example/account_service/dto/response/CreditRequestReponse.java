package com.example.account_service.dto.response;

import com.example.common_service.constant.AccountStatus;
import com.example.common_service.constant.AccountType;
import com.example.common_service.constant.CreditRequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class CreditRequestReponse {
    private String id;
    private String cifCode;
    private String occupation;
    private BigDecimal monthlyIncome;
    private String cartTypeId; // ex: "VISA", "MASTER", etc.
    private CreditRequestStatus status; // PENDING, APPROVED, REJECTED
    private LocalDateTime createdAt;
}
