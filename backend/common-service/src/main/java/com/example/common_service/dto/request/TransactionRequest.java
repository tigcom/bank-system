package com.example.common_service.dto.request;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionRequest implements Serializable {

    private String fromAccountNumber;

    private String toAccountNumber;

    private BigDecimal amount;

    private String description;

    private LocalDateTime timestamp;

    private String status;

    private String type;


}
