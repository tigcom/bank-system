package com.example.customer_service.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "DTO đăng nhập khách hàng")
public class LoginCustomerDTO {

    @Schema(description = "Tên đăng nhập", example = "nguyenvana123", required = true)
    @NotBlank(message = "Tên đăng nhập không được để trống")
    private String username;

    @Schema(description = "Mật khẩu khách hàng", example = "password", required = true)
    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;
}
