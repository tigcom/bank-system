package com.example.account_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatisticResponse {
    private AccountStatisticResponse accountStatistics;
    private SavingsStatisticResponse savingsStatistics;
    private CreditRequestStatisticResponse creditRequestStatistics;
    
    // Key metrics cho dashboard
    private Long totalCustomers;
    private BigDecimal totalAssets;
    private Long newAccountsThisMonth;
    private Long newAccountsLastMonth;
    private Double monthlyGrowthRate;
    
    // Top performers
    private List<TopPerformingProduct> topProducts;
    
    @Data
    @Builder
    public static class TopPerformingProduct {
        private String productName;
        private String productType;
        private Long accountCount;
        private BigDecimal totalValue;
    }
    
    // Recent activities
    private List<RecentActivity> recentActivities;
    
    @Data
    @Builder
    public static class RecentActivity {
        private String activityType;
        private String description;
        private String timestamp;
        private Long count;
    }
} 