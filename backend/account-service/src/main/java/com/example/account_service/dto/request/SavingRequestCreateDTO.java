package com.example.account_service.dto.request;

import com.example.common_service.constant.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingRequestCreateDTO {

    @Schema(description = "Số tài khoản nguồn chuyển tiền", example = "1234567890", required = true)
    @NotBlank(message = "Account number source cannot be blank")
    private String accountNumberSource;

    @Schema(description = "Số tiền gửi ban đầu", example = "1000000", required = true)
    @NotNull(message = "Account status is required")
    private BigDecimal initialDeposit;

    @Schema(description = "Kỳ hạn gửi (tính theo tháng)", example = "12", required = true, minimum = "1")
    @NotNull(message = "Term (in months) is required")
    @Min(value = 1, message = "Term must be at least 1 month")
    private Integer term; // kỳ hạn (tháng)

    private BigDecimal interestRate;
}
