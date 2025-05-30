package com.example.common_service.dto.request;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class CreateAccountSavingRequest implements Serializable {

    private String fromAccountNumber;

    private BigDecimal amount;

    private String description;

    private String currency;
}
