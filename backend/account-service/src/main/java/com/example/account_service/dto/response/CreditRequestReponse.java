package com.example.account_service.dto.response;

import com.example.common_service.constant.CreditRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreditRequestReponse {

    @Schema(description = "ID yêu cầu tín dụng", example = "123e4567-e89b-12d3-a456-426614174000")
    private String id;

    @Schema(description = "Mã CIF khách hàng", example = "CIF987654321")
    private String cifCode;

    @Schema(description = "Nghề nghiệp của khách hàng", example = "Software Engineer")
    private String occupation;

    @Schema(description = "Thu nhập hàng tháng của khách hàng", example = "1500.00")
    private BigDecimal monthlyIncome;

    @Schema(description = "Loại thẻ tín dụng (VISA, MASTER, etc.)", example = "VISA")
    private String cartTypeId;

    @Schema(description = "Trạng thái yêu cầu tín dụng", example = "PENDING")
    private CreditRequestStatus status;

}
