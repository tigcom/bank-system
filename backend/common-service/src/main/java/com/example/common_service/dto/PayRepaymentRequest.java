package com.example.common_service.dto;

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
public class PayRepaymentRequest implements Serializable {
    private String fromAccountNumber;    // Tài khoản khách trả
    private BigDecimal amount;           // Số tiền trả (có thể trả đủ hoặc trả một phần)
    private String currency;             // Loại tiền tệ
    private String description;
}
