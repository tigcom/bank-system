package com.example.customer_service.dtos;

import com.example.customer_service.ultils.MessageKeys;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResetPasswordDTO implements Serializable {

    @NotBlank(message = "{" + MessageKeys.NOT_BLANK_PASSWORD + "}")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[!@#$%^&*])[A-Za-z0-9!@#$%^&*]{8,}$",
            message = "{" + MessageKeys.PASSWORD_PATTERN + "}"
    )
    private String newPassword;

    @NotBlank(message = "{" + MessageKeys.NOT_BLANK_CONFIRM_PASSWORD + "}")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[!@#$%^&*])[A-Za-z0-9!@#$%^&*]{8,}$",
            message = "{" + MessageKeys.PASSWORD_PATTERN + "}"
    )
    private String confirmPassword;
}

