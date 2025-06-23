package com.example.transaction_service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BillDetailsResponse {
    private String billId;
    private String customerName;
    private String provider;
    private BigDecimal amount;
    private String status;
}
