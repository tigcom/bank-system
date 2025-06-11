package com.example.transaction_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Chi tiết thông tin giao dịch")
public class TransactionDTO {

    @Schema(description = "ID của giao dịch (UUID)", example = "a1b2c3d4-e5f6-7890-abcd-1234567890ef")
    private String id;

    @Schema(description = "Số tài khoản gửi", example = "100000001")
    @NotBlank(message = "From account number is required")
    private String fromAccountNumber;

    @Schema(description = "Số tài khoản nhận", example = "100000002")
    @NotBlank(message = "To account number is required")
    private String toAccountNumber;

    @Schema(description = "Số tiền giao dịch", example = "500000")
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", inclusive = true, message = "Amount must be at least 0.01")
    private BigDecimal amount;

    @Schema(description = "Nội dung giao dịch", example = "Thanh toán tiền điện")
    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    @Schema(description = "Thời gian thực hiện giao dịch", example = "2025-05-29T10:30:00")
    @PastOrPresent(message = "Timestamp cannot be in the future")
    private LocalDateTime timestamp;

    @Schema(description = "Trạng thái giao dịch", example = "SUCCESS")
    @NotBlank(message = "Status is required")
    @Pattern(regexp = "PENDING|SUCCESS|FAILED", message = "Status must be one of: PENDING, SUCCESS, FAILED")
    private String status;

    @Schema(description = "Loại giao dịch", example = "TRANSFER")
    @NotBlank(message = "Transaction type is required")
    @Pattern(regexp = "TRANSFER|DEPOSIT|WITHDRAW", message = "Type must be one of: TRANSFER, DEPOSIT, WITHDRAW")
    private String type;

    @Schema(description = "Loại tiền tệ", example = "VND")
    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "VND|USD|EUR", message = "Currency must be VND, USD, or EUR")
    private String currency;

    @Schema(description = "Mã tham chiếu giao dịch (duy nhất)", example = "TXN123456789")
    @NotBlank(message = "Reference code is required")
    @Size(max = 100, message = "Reference code must not exceed 100 characters")
    private String referenceCode;

    @Schema(description = "Lý do thất bại (nếu có)", example = "Tài khoản không đủ số dư")
    private String failedReason;

    private String bankType;
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String createdBy;

    private String updatedBy;
}
