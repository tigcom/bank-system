package com.example.transaction_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionStatsResponse {
    private long totalTransactions;
    private BigDecimal totalAmount;

    private long successCount;
    private long failedCount;
    private long pendingCount;

    private List<TransactionTypeSummary> transactionTypeSummary;
    private List<TopCustomerStats> topCustomers;
    private List<RecentTransaction> latestTransactions;

//    Thống kê theo các loại giao dịch
    @Data
    @Builder
    public static class TransactionTypeSummary {
        private String transactionType;
        private long count;
        private BigDecimal totalAmount;
    }

//    Thống kê số top khách hàng
    @Data
    @Builder
    public static class TopCustomerStats {
        private String cifCode;
        private String name;
        private long transactionCount;
        private BigDecimal totalAmount;
    }

//    Các giao dịch gần nhất
    @Data
    @Builder
    public static class RecentTransaction {
        private String transactionId;
        private String fromAccount;
        private String toAccount;
        private BigDecimal amount;
        private String type;
        private String status;
        private LocalDateTime createdAt;
    }
}
