package com.example.customer_service.dtos;

import com.example.customer_service.ultils.MessageKeys;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "DTO yêu cầu KYC cho khách hàng")
public class KycRequest {

    @Schema(description = "ID khách hàng", example = "123", required = true)
    @NotNull(message = "CustomerId không được để trống")
    private Long customerId;

    @Schema(description = "Số CMND/CCCD", example = "012345678", required = true)
    @NotBlank(message = "{" + MessageKeys.NOT_BLANK_IDENTITY_NUMBER + "}")
    @Pattern(regexp = "\\d{9}|\\d{12}", message = "{" + MessageKeys.IDENTITY_NUMBER_PATTERN + "}")
    private String identityNumber;

    @Schema(description = "Họ tên khách hàng", example = "Nguyễn Văn A", required = true)
    @NotBlank(message = "{" + MessageKeys.NOT_BLANK_FULL_NAME + "}")
    private String fullName;

    @Schema(description = "Ngày sinh", example = "1990-01-01", required = true)
    @NotNull(message = "{" + MessageKeys.NOT_NULL_DOB + "}")
    private LocalDate dateOfBirth;

    @Schema(description = "Giới tính", example = "male", required = true)
    @NotNull(message = "{" + MessageKeys.INVALID_GENDER + "}")
    private String gender;
}
