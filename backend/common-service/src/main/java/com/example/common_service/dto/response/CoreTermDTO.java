package com.example.common_service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
@Data
@Builder
public class CoreTermDTO {

    private Long termId;
    private Integer termValueMonths;
    private BigDecimal interestRate;
    private Boolean isActive;
}
