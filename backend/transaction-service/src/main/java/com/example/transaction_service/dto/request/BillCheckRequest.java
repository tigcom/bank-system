package com.example.transaction_service.dto.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BillCheckRequest {
    private String billType; // "ELECTRICITY", "TELEPHONE"
    private String customerCode;
}
