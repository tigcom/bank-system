package com.example.account_service.controller;

import com.example.account_service.service.impl.StatisticServiceImpl;
import com.example.account_service.dto.response.AccountStatisticResponse;
import com.example.account_service.dto.response.SavingsStatisticResponse;
import com.example.account_service.dto.response.CreditRequestStatisticResponse;
import com.example.account_service.dto.response.AccountGrowthStatisticResponse;
import com.example.account_service.dto.response.DashboardStatisticResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import com.example.account_service.entity.SavingsAccount;
import org.springframework.format.annotation.DateTimeFormat;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/statistic")
@Tag(name = "Admin Statistics", description = "API thống kê cho admin dashboard")
public class StatisticController {

    private final StatisticServiceImpl statisticService;
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/accounts/overview")
    @Operation(summary = "Thống kê tổng quan tài khoản", 
               description = "Lấy thống kê số lượng tài khoản theo loại và trạng thái")
    public ResponseEntity<AccountStatisticResponse> getAccountStatistics() {
        return ResponseEntity.ok(statisticService.getAccountStatistics());
    }
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/accounts/growth")
    @Operation(summary = "Thống kê tăng trưởng tài khoản theo khoảng thời gian",
               description = "Lấy thống kê số lượng tài khoản được tạo từ ngày đến ngày. Nếu không chọn, mặc định 30 ngày gần nhất")
    public ResponseEntity<AccountGrowthStatisticResponse> getAccountGrowthStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(statisticService.getAccountGrowthStatistics(fromDate, toDate));
    }
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/savings/overview")
    @Operation(summary = "Thống kê tài khoản tiết kiệm",
               description = "Lấy thống kê tổng quan về tài khoản tiết kiệm")
    public ResponseEntity<SavingsStatisticResponse> getSavingsStatistics() {
        return ResponseEntity.ok(statisticService.getSavingsStatistics());
    }
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/savings/maturity")
    @Operation(summary = "Thống kê tài khoản tiết kiệm đến hạn",
               description = "Lấy danh sách tài khoản tiết kiệm đến hạn trong khoảng thời gian")
    public ResponseEntity<List<SavingsAccount>> getMaturitySavingsAccounts(
            @RequestParam LocalDate fromDate,
            @RequestParam LocalDate toDate) {
        return ResponseEntity.ok(statisticService.getMaturitySavingsAccounts(fromDate, toDate));
    }
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/credit-requests/overview")
    @Operation(summary = "Thống kê yêu cầu mở tài khoản credit",
               description = "Lấy thống kê về các yêu cầu mở tài khoản credit")
    public ResponseEntity<CreditRequestStatisticResponse> getCreditRequestStatistics() {
        return ResponseEntity.ok(statisticService.getCreditRequestStatistics());
    }
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/dashboard")
    @Operation(summary = "Thống kê tổng hợp cho dashboard",
               description = "Lấy tất cả thống kê cần thiết cho dashboard admin")
    public ResponseEntity<DashboardStatisticResponse> getDashboardStatistics() {
        return ResponseEntity.ok(statisticService.getDashboardStatistics());
    }
}
