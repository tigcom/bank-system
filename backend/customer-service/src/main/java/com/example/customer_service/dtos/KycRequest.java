package com.example.customer_service.dtos;

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
    @NotBlank(message = "Số CMND/CCCD không được để trống")
    private String identityNumber;

    @Schema(description = "Họ tên khách hàng", example = "Nguyễn Văn A", required = true)
    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    @Schema(description = "Ngày sinh", example = "1990-01-01", required = true)
    @NotNull(message = "Ngày sinh không được để trống")
    private LocalDate dateOfBirth;

    @Schema(description = "Giới tính", example = "male", required = true)
    @NotBlank(message = "Giới tính không được để trống")
    private String gender;
}
