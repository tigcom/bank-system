package com.example.mock_provider_server.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BillDetailsResponse {
    private String billId;
    private String customerName;
    private String customerCode;
    private String provider;
    private BigDecimal amount;
    private String status;
}
