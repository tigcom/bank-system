package com.example.customer_service.responses;

import lombok.Data;

@Data
public class CustomerGrowthResponse {
    private long totalNewCustomers;
    private long previousPeriodCustomers;
    private double growthRate;
}