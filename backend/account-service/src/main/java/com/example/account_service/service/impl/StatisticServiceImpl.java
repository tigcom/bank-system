package com.example.account_service.service.impl;

import com.example.account_service.dto.response.*;
import com.example.account_service.entity.Account;
import com.example.account_service.entity.CreditRequest;
import com.example.account_service.entity.SavingsAccount;
import com.example.account_service.repository.AccountRepository;
import com.example.account_service.repository.CreditRequestRepository;
import com.example.account_service.repository.SavingsAccountRepository;

import com.example.common_service.constant.AccountStatus;
import com.example.common_service.constant.AccountType;
import com.example.common_service.constant.CreditRequestStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticServiceImpl {

    private final AccountRepository accountRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final CreditRequestRepository creditRequestRepository;

    /**
     * Helper method để lọc bỏ tài khoản admin và master từ thống kê
     */
    private List<Account> filterExcludeAdminAndMasterAccounts(List<Account> accounts) {
        return accounts.stream()
                .filter(account -> account.getAccountType() != AccountType.MASTER)
                .filter(account -> !isAdminAccount(account.getCifCode()))
                .collect(Collectors.toList());
    }
    
    /**
     * Kiểm tra xem CIF code có phải là tài khoản admin không
     */
    private boolean isAdminAccount(String cifCode) {
        if (cifCode == null) return false;
        String upperCifCode = cifCode.toUpperCase();
        return upperCifCode.startsWith("ADMIN") || upperCifCode.startsWith("ADM");
    }

    public AccountStatisticResponse getAccountStatistics() {
        List<Account> allAccounts = accountRepository.findAll();
        // Lọc bỏ tài khoản admin và master
        List<Account> filteredAccounts = filterExcludeAdminAndMasterAccounts(allAccounts);
        
        Map<String, Long> accountsByType = filteredAccounts.stream()
                .collect(Collectors.groupingBy(
                        account -> account.getAccountType().name(),
                        Collectors.counting()
                ));
        
        Map<String, Long> accountsByStatus = filteredAccounts.stream()
                .collect(Collectors.groupingBy(
                        account -> account.getStatus().name(),
                        Collectors.counting()
                ));
        
        long activeAccounts = filteredAccounts.stream()
                .filter(account -> account.getStatus() == AccountStatus.ACTIVE)
                .count();
        
        long inactiveAccounts = filteredAccounts.size() - activeAccounts;
        
        return AccountStatisticResponse.builder()
                .totalAccounts((long) filteredAccounts.size())
                .accountsByType(accountsByType)
                .accountsByStatus(accountsByStatus)
                .activeAccounts(activeAccounts)
                .inactiveAccounts(inactiveAccounts)
                .build();
    }

    public AccountGrowthStatisticResponse getAccountGrowthStatistics(LocalDate fromDate, LocalDate toDate) {
        // Nếu không có fromDate và toDate, mặc định 30 ngày gần nhất
        if (fromDate == null && toDate == null) {
            toDate = LocalDate.now();
            fromDate = toDate.minusDays(29); // 30 ngày (bao gồm hôm nay)
        } else if (fromDate == null) {
            fromDate = toDate.minusDays(29);
        } else if (toDate == null) {
            toDate = LocalDate.now();
        }
        
        // Đảm bảo fromDate <= toDate
        if (fromDate.isAfter(toDate)) {
            LocalDate temp = fromDate;
            fromDate = toDate;
            toDate = temp;
        }
        
        LocalDateTime fromDateTime = fromDate.atStartOfDay();
        LocalDateTime toDateTime = toDate.atTime(23, 59, 59);
        
        List<Account> allAccountsInPeriod = accountRepository.findAll().stream()
                .filter(account -> account.getCreatedDate() != null &&
                        !account.getCreatedDate().isBefore(fromDateTime) &&
                        !account.getCreatedDate().isAfter(toDateTime))
                .collect(Collectors.toList());
        
        // Lọc bỏ tài khoản admin và master
        List<Account> accountsInPeriod = filterExcludeAdminAndMasterAccounts(allAccountsInPeriod);
        
        Map<LocalDate, Long> dailyAccountCreation = accountsInPeriod.stream()
                .collect(Collectors.groupingBy(
                        account -> account.getCreatedDate().toLocalDate(),
                        Collectors.counting()
                ));
        
        Map<String, Map<LocalDate, Long>> growthByAccountType = new HashMap<>();
        for (AccountType type : AccountType.values()) {
            if (type != AccountType.MASTER) { // Loại bỏ MASTER khỏi thống kê
                Map<LocalDate, Long> typeGrowth = accountsInPeriod.stream()
                        .filter(account -> account.getAccountType() == type)
                        .collect(Collectors.groupingBy(
                                account -> account.getCreatedDate().toLocalDate(),
                                Collectors.counting()
                        ));
                growthByAccountType.put(type.name(), typeGrowth);
            }
        }
        
        // Tính growth rate so với kỳ trước (cùng số ngày trước đó)
        int periodDays = (int) ChronoUnit.DAYS.between(fromDate, toDate) + 1;
        LocalDate previousPeriodStart = fromDate.minusDays(periodDays);
        LocalDate previousPeriodEnd = fromDate.minusDays(1);
        LocalDateTime previousFromDateTime = previousPeriodStart.atStartOfDay();
        LocalDateTime previousToDateTime = previousPeriodEnd.atTime(23, 59, 59);
        
        List<Account> allPreviousPeriodAccounts = accountRepository.findAll().stream()
                .filter(account -> account.getCreatedDate() != null &&
                        !account.getCreatedDate().isBefore(previousFromDateTime) &&
                        !account.getCreatedDate().isAfter(previousToDateTime))
                .collect(Collectors.toList());
        
        // Lọc bỏ tài khoản admin và master khỏi kỳ trước
        List<Account> previousPeriodAccounts = filterExcludeAdminAndMasterAccounts(allPreviousPeriodAccounts);
        long previousPeriodCount = previousPeriodAccounts.size();
        
        double growthRate = previousPeriodCount > 0 ? 
                ((double) (accountsInPeriod.size() - previousPeriodCount) / previousPeriodCount) * 100 : 0;
        
        // Tạo daily growth details
        List<AccountGrowthStatisticResponse.DailyGrowth> dailyGrowthDetails = new ArrayList<>();
        Long previousDayTotal = null;
        
        for (LocalDate date = fromDate; !date.isAfter(toDate); date = date.plusDays(1)) {
            final LocalDate currentDate = date;
            List<Account> dayAccounts = accountsInPeriod.stream()
                    .filter(account -> account.getCreatedDate().toLocalDate().equals(currentDate))
                    .collect(Collectors.toList());
            
            long totalCreated = dayAccounts.size();
            
            // Tính growth rate so với ngày trước
            Double dailyGrowthRate = null;
            if (previousDayTotal != null && previousDayTotal > 0) {
                dailyGrowthRate = ((double) (totalCreated - previousDayTotal) / previousDayTotal) * 100;
            }
            
            AccountGrowthStatisticResponse.DailyGrowth dailyGrowth = AccountGrowthStatisticResponse.DailyGrowth.builder()
                    .date(currentDate)
                    .totalCreated(totalCreated)
                    .paymentAccountsCreated(dayAccounts.stream()
                            .filter(acc -> acc.getAccountType() == AccountType.PAYMENT)
                            .count())
                    .savingAccountsCreated(dayAccounts.stream()
                            .filter(acc -> acc.getAccountType() == AccountType.SAVING)
                            .count())
                    .creditAccountsCreated(dayAccounts.stream()
                            .filter(acc -> acc.getAccountType() == AccountType.CREDIT)
                            .count())
                    .growthRateFromPreviousDay(dailyGrowthRate)
                    .build();
            dailyGrowthDetails.add(dailyGrowth);
            
            previousDayTotal = totalCreated;
        }
        
        return AccountGrowthStatisticResponse.builder()
                .dailyAccountCreation(dailyAccountCreation)
                .growthByAccountType(growthByAccountType)
                .totalAccountsInPeriod((long) accountsInPeriod.size())
                .growthRate(growthRate)
                .fromDate(fromDate)
                .toDate(toDate)
                .periodDays(periodDays)
                .dailyGrowthDetails(dailyGrowthDetails)
                .build();
    }

    public SavingsStatisticResponse getSavingsStatistics() {
        List<SavingsAccount> allSavingsAccounts = savingsAccountRepository.findAll();
        // Lọc bỏ tài khoản admin và master từ savings accounts
        List<SavingsAccount> filteredSavingsAccounts = allSavingsAccounts.stream()
                .filter(savingsAccount -> !isAdminAccount(savingsAccount.getCifCode()))
                .collect(Collectors.toList());
        
        BigDecimal totalBalance = filteredSavingsAccounts.stream()
                .map(SavingsAccount::getInitialDeposit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        Map<Integer, Long> accountsByTerm = filteredSavingsAccounts.stream()
                .filter(account -> account.getTerm() != null)
                .collect(Collectors.groupingBy(
                        account -> account.getTerm().getTermValueMonths(),
                        Collectors.counting()
                ));
        
        Map<String, Long> accountsByInterestPaymentType = filteredSavingsAccounts.stream()
                .collect(Collectors.groupingBy(
                        account -> account.getInterestPaymentType().name(),
                        Collectors.counting()
                ));
        
        Map<String, Long> accountsByRenewOption = filteredSavingsAccounts.stream()
                .collect(Collectors.groupingBy(
                        account -> account.getRenewOption().name(),
                        Collectors.counting()
                ));
        
        // Tài khoản sắp đến hạn trong 30 ngày
        LocalDateTime thirtyDaysFromNow = LocalDateTime.now().plusDays(30);
        long accountsNearMaturity = filteredSavingsAccounts.stream()
                .filter(account -> account.getMaturityDate() != null &&
                        account.getMaturityDate().isBefore(thirtyDaysFromNow) &&
                        account.getStatus() == AccountStatus.ACTIVE)
                .count();
        
        BigDecimal averageBalance = filteredSavingsAccounts.isEmpty() ? BigDecimal.ZERO :
                totalBalance.divide(BigDecimal.valueOf(filteredSavingsAccounts.size()), 2, RoundingMode.HALF_UP);
        
        return SavingsStatisticResponse.builder()
                .totalSavingsAccounts((long) filteredSavingsAccounts.size())
                .totalSavingsBalance(totalBalance)
                .accountsByTerm(accountsByTerm)
                .accountsByInterestPaymentType(accountsByInterestPaymentType)
                .accountsByRenewOption(accountsByRenewOption)
                .accountsNearMaturity(accountsNearMaturity)
                .averageBalance(averageBalance)
                .build();
    }

    public List<SavingsAccount> getMaturitySavingsAccounts(LocalDate fromDate, LocalDate toDate) {
        LocalDateTime fromDateTime = fromDate.atStartOfDay();
        LocalDateTime toDateTime = toDate.atTime(23, 59, 59);
        
        return savingsAccountRepository.findAll().stream()
                .filter(account -> account.getMaturityDate() != null &&
                        account.getMaturityDate().isAfter(fromDateTime) &&
                        account.getMaturityDate().isBefore(toDateTime) &&
                        account.getStatus() == AccountStatus.ACTIVE)
                .filter(account -> !isAdminAccount(account.getCifCode())) // Lọc bỏ admin accounts
                .collect(Collectors.toList());
    }

    public CreditRequestStatisticResponse getCreditRequestStatistics() {
        List<CreditRequest> allCreditRequests = creditRequestRepository.findAll();
        // Lọc bỏ các credit request từ admin accounts
        List<CreditRequest> filteredCreditRequests = allCreditRequests.stream()
                .filter(request -> !isAdminAccount(request.getCifCode()))
                .collect(Collectors.toList());
        
        Map<String, Long> requestsByStatus = filteredCreditRequests.stream()
                .collect(Collectors.groupingBy(
                        request -> request.getStatus().name(),
                        Collectors.counting()
                ));
        
        long pendingRequests = filteredCreditRequests.stream()
                .filter(request -> request.getStatus() == CreditRequestStatus.PENDING)
                .count();
        
        long approvedRequests = filteredCreditRequests.stream()
                .filter(request -> request.getStatus() == CreditRequestStatus.APPROVED)
                .count();
        
        long rejectedRequests = filteredCreditRequests.stream()
                .filter(request -> request.getStatus() == CreditRequestStatus.REJECTED)
                .count();
        
        double approvalRate = (approvedRequests + rejectedRequests) > 0 ?
                ((double) approvedRequests / (approvedRequests + rejectedRequests)) * 100 : 0;
        
        BigDecimal averageIncome = filteredCreditRequests.stream()
                .filter(request -> request.getMonthlyIncome() != null)
                .map(CreditRequest::getMonthlyIncome)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(Math.max(1, filteredCreditRequests.size())), 2, RoundingMode.HALF_UP);
        
        Map<String, Long> requestsByCardType = filteredCreditRequests.stream()
                .filter(request -> request.getCartTypeId() != null)
                .collect(Collectors.groupingBy(
                        CreditRequest::getCartTypeId,
                        Collectors.counting()
                ));
        
        return CreditRequestStatisticResponse.builder()
                .totalCreditRequests((long) filteredCreditRequests.size())
                .requestsByStatus(requestsByStatus)
                .pendingRequests(pendingRequests)
                .approvedRequests(approvedRequests)
                .rejectedRequests(rejectedRequests)
                .approvalRate(approvalRate)
                .averageRequestedIncome(averageIncome)
                .requestsByCardType(requestsByCardType)
                .build();
    }

    public DashboardStatisticResponse getDashboardStatistics() {
        AccountStatisticResponse accountStats = getAccountStatistics();
        SavingsStatisticResponse savingsStats = getSavingsStatistics();
        CreditRequestStatisticResponse creditStats = getCreditRequestStatistics();
        
        // Tính toán metrics cho dashboard
        Long newAccountsThisMonth = getNewAccountsInMonth(LocalDate.now());
        Long newAccountsLastMonth = getNewAccountsInMonth(LocalDate.now().minusMonths(1));
        
        Double monthlyGrowthRate = newAccountsLastMonth > 0 ?
                ((double) (newAccountsThisMonth - newAccountsLastMonth) / newAccountsLastMonth) * 100 : 0;
        
        // Tổng tài sản (tạm tính từ savings accounts)
        BigDecimal totalAssets = savingsStats.getTotalSavingsBalance();
        
        return DashboardStatisticResponse.builder()
                .accountStatistics(accountStats)
                .savingsStatistics(savingsStats)
                .creditRequestStatistics(creditStats)
                .totalCustomers(getTotalUniqueCustomers())
                .totalAssets(totalAssets)
                .newAccountsThisMonth(newAccountsThisMonth)
                .newAccountsLastMonth(newAccountsLastMonth)
                .monthlyGrowthRate(monthlyGrowthRate)
                .topProducts(getTopPerformingProducts())
                .recentActivities(getRecentActivities())
                .build();
    }
    
    private Long getNewAccountsInMonth(LocalDate month) {
        LocalDateTime startOfMonth = month.withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = month.withDayOfMonth(month.lengthOfMonth()).atTime(23, 59, 59);
        
        List<Account> monthlyAccounts = accountRepository.findAll().stream()
                .filter(account -> account.getCreatedDate() != null &&
                        account.getCreatedDate().isAfter(startOfMonth) &&
                        account.getCreatedDate().isBefore(endOfMonth))
                .collect(Collectors.toList());
        
        // Lọc bỏ tài khoản admin và master
        List<Account> filteredMonthlyAccounts = filterExcludeAdminAndMasterAccounts(monthlyAccounts);
        
        return (long) filteredMonthlyAccounts.size();
    }
    
    private Long getTotalUniqueCustomers() {
        List<Account> allAccounts = accountRepository.findAll();
        // Lọc bỏ tài khoản admin và master
        List<Account> filteredAccounts = filterExcludeAdminAndMasterAccounts(allAccounts);
        
        return filteredAccounts.stream()
                .map(Account::getCifCode)
                .distinct()
                .count();
    }
    
    private List<DashboardStatisticResponse.TopPerformingProduct> getTopPerformingProducts() {
        List<Account> allAccounts = accountRepository.findAll();
        // Lọc bỏ tài khoản admin và master
        List<Account> filteredAccounts = filterExcludeAdminAndMasterAccounts(allAccounts);
        
        Map<String, Long> productCounts = filteredAccounts.stream()
                .collect(Collectors.groupingBy(
                        account -> account.getAccountType().name(),
                        Collectors.counting()
                ));
        
        return productCounts.entrySet().stream()
                .map(entry -> DashboardStatisticResponse.TopPerformingProduct.builder()
                        .productName(entry.getKey())
                        .productType("ACCOUNT")
                        .accountCount(entry.getValue())
                        .totalValue(BigDecimal.ZERO) // Có thể tính toán thêm
                        .build())
                .sorted((a, b) -> Long.compare(b.getAccountCount(), a.getAccountCount()))
                .limit(5)
                .collect(Collectors.toList());
    }
    
    private List<DashboardStatisticResponse.RecentActivity> getRecentActivities() {
        // Simplified recent activities - có thể mở rộng thêm
        List<DashboardStatisticResponse.RecentActivity> activities = new ArrayList<>();
        
        // Recent account creations (lọc bỏ admin và master)
        List<Account> recentAccountsList = accountRepository.findAll().stream()
                .filter(account -> account.getCreatedDate() != null &&
                        account.getCreatedDate().isAfter(LocalDateTime.now().minusDays(1)))
                .collect(Collectors.toList());
        
        List<Account> filteredRecentAccounts = filterExcludeAdminAndMasterAccounts(recentAccountsList);
        long recentAccounts = filteredRecentAccounts.size();
        
        if (recentAccounts > 0) {
            activities.add(DashboardStatisticResponse.RecentActivity.builder()
                    .activityType("ACCOUNT_CREATION")
                    .description("Tài khoản mới được tạo trong 24h qua")
                    .timestamp(LocalDateTime.now().toString())
                    .count(recentAccounts)
                    .build());
        }
        
        // Recent credit requests (lọc bỏ admin)
        long recentCreditRequests = creditRequestRepository.findAll().stream()
                .filter(request -> request.getCreatedDate() != null &&
                        request.getCreatedDate().isAfter(LocalDateTime.now().minusDays(1)))
                .filter(request -> !isAdminAccount(request.getCifCode()))
                .count();
        
        if (recentCreditRequests > 0) {
            activities.add(DashboardStatisticResponse.RecentActivity.builder()
                    .activityType("CREDIT_REQUEST")
                    .description("Yêu cầu mở thẻ tín dụng mới trong 24h qua")
                    .timestamp(LocalDateTime.now().toString())
                    .count(recentCreditRequests)
                    .build());
        }
        
        return activities;
    }
} 