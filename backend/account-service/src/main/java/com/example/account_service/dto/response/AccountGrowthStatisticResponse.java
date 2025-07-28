package com.example.account_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountGrowthStatisticResponse {
    private Map<LocalDate, Long> dailyAccountCreation; // Số tài khoản tạo mỗi ngày
    private Map<String, Map<LocalDate, Long>> growthByAccountType; // Tăng trưởng theo loại tài khoản theo ngày
    private Long totalAccountsInPeriod;
    private Double growthRate; // Tỷ lệ tăng trưởng so với kỳ trước
    private LocalDate fromDate;
    private LocalDate toDate;
    private Integer periodDays; // Số ngày trong khoảng thời gian
    
    @Data
    @Builder
    public static class DailyGrowth {
        private LocalDate date;
        private Long totalCreated;
        private Long paymentAccountsCreated;
        private Long savingAccountsCreated;
        private Long creditAccountsCreated;
        private Double growthRateFromPreviousDay; // Tỷ lệ tăng trưởng so với ngày trước
    }
    
    private List<DailyGrowth> dailyGrowthDetails;
} 