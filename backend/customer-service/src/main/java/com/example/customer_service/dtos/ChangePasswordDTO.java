package com.example.customer_service.dtos;

import com.example.customer_service.ultils.MessageKeys;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "DTO thay đổi mật khẩu khách hàng")
public class ChangePasswordDTO {

    @Schema(description = "Mật khẩu cũ", example = "NewPass456!", required = true)
    @NotBlank(message = "{" + MessageKeys.NOT_BLANK_PASSWORD + "}")
    private String currentPassword;

    @Schema(description = "Mật khẩu mới, ít nhất 8 ký tự, 1 chữ hoa, 1 ký tự đặc biệt", example = "NewPass456!", required = true)
    @NotBlank(message = "{" + MessageKeys.NOT_BLANK_PASSWORD + "}")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[!@#$%^&*])[A-Za-z0-9!@#$%^&*]{8,}$",
            message = "Mật khẩu mới phải có ít nhất 8 ký tự, bao gồm 1 chữ hoa và 1 ký tự đặc biệt (!@#$%^&*)"
    )
    private String newPassword;

    @Schema(description = "Xác nhận mật khẩu mới", example = "NewPass456!", required = true)
    @NotBlank(message = "{" + MessageKeys.NOT_BLANK_CONFIRM_PASSWORD+ "}")
    private String confirmPassword;
}