package com.example.common_service.dto.request;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
public class WithdrawAccountSavingRequest implements Serializable {
  private static final long serialVersionUID = 1L;
    private String toAccountNumber;

    private BigDecimal amount;

    private String description;

    private String currency;
}
