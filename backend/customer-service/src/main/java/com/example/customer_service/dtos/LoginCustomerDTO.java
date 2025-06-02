package com.example.customer_service.dtos;

import com.example.customer_service.ultils.MessageKeys;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "DTO đăng nhập khách hàng")
public class LoginCustomerDTO {

    @Schema(description = "Tên đăng nhập", example = "nguyenvana123", required = true)
    @NotBlank(message = "{" + MessageKeys.LOGIN_NOT_BLANK_USERNAME+ "}")
    private String username;

    @Schema(description = "Mật khẩu khách hàng", example = "password", required = true)
    @NotBlank(message = "{" + MessageKeys.LOGIN_NOT_BLANK_PASSWORD+ "}")
    private String password;
}
