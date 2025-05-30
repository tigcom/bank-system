package com.example.customer_service.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "DTO cập nhật thông tin khách hàng")
public class UpdateCustomerDTO {

    @Schema(description = "Họ tên khách hàng", example = "Nguyễn Văn A", required = true)
    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    @Schema(description = "Địa chỉ khách hàng", example = "123 Đường ABC", required = true)
    @NotBlank(message = "Địa chỉ không được để trống")
    private String address;

    @Schema(description = "Số điện thoại khách hàng", example = "0901234567", required = true)
    @NotBlank(message = "Số điện thoại không được để trống")
    private String phoneNumber;
}
