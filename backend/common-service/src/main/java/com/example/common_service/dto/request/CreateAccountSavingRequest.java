package com.example.common_service.dto.request;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder

public class CreateAccountSavingRequest implements Serializable {

    private String fromAccountNumber;

    private BigDecimal amount;

    private String description;

    private String currency;
}
