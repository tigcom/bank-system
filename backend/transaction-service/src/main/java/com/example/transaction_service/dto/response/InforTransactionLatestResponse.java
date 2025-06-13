package com.example.transaction_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InforTransactionLatestResponse {
    private String accountNumber;
    private String customerName;
}
