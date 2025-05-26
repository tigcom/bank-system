package com.example.customer_service.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "DTO thay đổi mật khẩu khách hàng")
public class ChangePasswordDTO {

    @Schema(description = "ID khách hàng", example = "123")
    private Long customerId;

    @Schema(description = "Mật khẩu cũ", example = "oldPass123", required = true)
    @NotBlank(message = "Mật khẩu cũ không được để trống")
    private String oldPassword;

    @Schema(description = "Mật khẩu mới, ít nhất 8 ký tự", example = "newPass456", required = true)
    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 8, message = "Mật khẩu mới phải có ít nhất 8 ký tự")
    private String newPassword;

    @Schema(description = "Xác nhận mật khẩu mới", example = "newPass456", required = true)
    @NotBlank(message = "Xác nhận mật khẩu không được để trống")
    private String confirmPassword;
}
