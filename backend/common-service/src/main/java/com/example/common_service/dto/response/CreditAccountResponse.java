package com.example.common_service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class CreditAccountResponse {
    private String accountNumber;
    private String cifCode;
    private String accountType;
    private BigDecimal balance;
    private String status;
    private LocalDate openedDate;
    private BigDecimal creditLimit; // Hạn mức tín dụng tối đa ngân hàng cấp
    private BigDecimal currentDebt = BigDecimal.ZERO; // Số tiền mà khách đã sử dụng (nợ hiện tại)
    private String typeName;
    private String imageUrl;
    private String cardID;
}
