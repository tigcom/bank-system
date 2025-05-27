package com.example.account_service.dto.request;

import com.example.account_service.service.BaseAccountCreateDTO;
import com.example.common_service.constant.AccountStatus;
import com.example.common_service.constant.AccountType;
import com.example.common_service.constant.CreditRequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditRequestCreateDTO   {
    @NotBlank(message = "Nghề nghiệp không được để trống")
    private String occupation;

    @NotNull(message = "Thu nhập hàng tháng không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Thu nhập hàng tháng phải lớn hơn 0")
    private BigDecimal monthlyIncome;

    @NotBlank(message = "Mã loại thẻ không được để trống")
    private String cartTypeId; // ex: "VISA", "MASTER", etc.
}
