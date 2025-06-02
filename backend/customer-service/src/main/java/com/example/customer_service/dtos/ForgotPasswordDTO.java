package com.example.customer_service.dtos;

import com.example.customer_service.ultils.MessageKeys;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPasswordDTO {

    @NotBlank(message = "{" + MessageKeys.NOT_BLANK_EMAIL + "}")
    @Pattern(regexp = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$", message = "{" + MessageKeys.INVALID_EMAIL + "}")
    private String email;
}
