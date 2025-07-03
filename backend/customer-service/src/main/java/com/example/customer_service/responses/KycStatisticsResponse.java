package com.example.customer_service.responses;

import lombok.Data;

@Data
public class KycStatisticsResponse {
    private long totalKycRequests;
    private long successfulKyc;
    private long failedKyc;
    private long pendingKyc;
}
