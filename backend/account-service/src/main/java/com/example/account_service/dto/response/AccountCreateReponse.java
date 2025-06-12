package com.example.account_service.dto.response;

import com.example.common_service.constant.AccountStatus;
import com.example.common_service.constant.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder

public class AccountCreateReponse {
    @Schema(description = "ID tài khoản", example = "123e4567-e89b-12d3-a456-426614174000")
    private String id;
    @Schema(description = "Số tài khoản", example = "00123456789")
    private String accountNumber;
    @Schema(description = "Loại tài khoản", example = "SAVING")
    private AccountType accountType;
    @Schema(description = "Mã CIF khách hàng", example = "CIF987654321")
    private String cifCode;
    @Schema(description = "Trạng thái tài khoản", example = "ACTIVE")
    private AccountStatus status;
    private String srcAccountNumber;
}
