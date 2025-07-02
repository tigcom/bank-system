package com.example.account_service.dto.kafkaMessage;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardRegistrationMessage {
    private String idRequest;
    private String cifCode;
    private String accountNumber;
    private BigDecimal creditLimit;
    private String cardType;
}
