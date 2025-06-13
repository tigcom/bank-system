package com.example.account_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditRequestCreateDTO  implements Serializable {
    private static final long serialVersionUID = 1L;
    @Schema(description = "Nghề nghiệp của khách hàng", example = "Giáo viên", required = true)
    @NotBlank(message = "Nghề nghiệp không được để trống")
    private String occupation;

    @Schema(description = "Thu nhập hàng tháng của khách hàng", example = "15000000.00", required = true)
    @NotNull(message = "Thu nhập hàng tháng không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Thu nhập hàng tháng phải lớn hơn 0")
    private BigDecimal monthlyIncome;

    @Schema(description = "Mã loại thẻ tín dụng (ví dụ: VISA, MASTER)", example = "VISA", required = true)
    @NotBlank(message = "Mã loại thẻ không được để trống")
    private String cartTypeId; // ex: "VISA", "MASTER", etc.
}
