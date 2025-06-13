package com.example.transaction_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BillPaymentRequest {
    @NotBlank(message = "Số tài khoản nguồn không được để trống")
    private String fromAccountNumber;

    @NotNull(message = "Số tiền không được để trống")
    @Positive(message = "Số tiền phải là số dương")
    private BigDecimal amount;

    @NotBlank(message = "Loại hóa đơn không được để trống")
    private String billType;
    @NotBlank(message = "Mã khách hàng của hóa đơn không được để trống")
    private String customerCode;

    private String description;

    @NotBlank(message = "Loại tiền tệ không được để trống")
    private String currency;
}
