package com.example.customer_service.dtos;

import com.example.customer_service.models.Gender;
import com.example.customer_service.ultils.MessageKeys;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@Schema(description = "Thông tin đăng ký khách hàng")
@JsonIgnoreProperties(ignoreUnknown = true)
public class RegisterCustomerDTO implements Serializable {

    @Schema(description = "Tên đăng nhập", example = "nguyenvana123", required = true)
    @NotBlank(message = "{" + MessageKeys.LOGIN_NOT_BLANK_USERNAME + "}")
    @Size(min = 3, max = 50, message = "{" + MessageKeys.USERNAME_SIZE + "}")
    @Pattern(regexp = "^[a-zA-Z0-9._-]{3,50}$", message = "{" + MessageKeys.USERNAME_PATTERN + "}")
    private String username;

    @Schema(description = "Họ tên khách hàng", example = "Nguyễn Văn A", required = true)
    @NotBlank(message = "{" + MessageKeys.NOT_BLANK_FULL_NAME + "}")
    private String fullName;

    @Schema(description = "Địa chỉ khách hàng", example = "123 Đường ABC, Quận 1", required = true)
    @NotBlank(message = "{" + MessageKeys.NOT_BLANK_ADDRESS+ "}")
    private String address;

    @Schema(description = "Số CMND/CCCD", example = "012345678", required = true)
    @NotBlank(message = "{" + MessageKeys.NOT_BLANK_IDENTITY_NUMBER + "}")
    @Pattern(regexp = "\\d{9}|\\d{12}", message = "{" + MessageKeys.IDENTITY_NUMBER_PATTERN + "}")
    private String identityNumber;

    @Schema(description = "Email", example = "john.doe@example.com", required = true)
    @NotBlank(message = "{" + MessageKeys.NOT_BLANK_EMAIL + "}")
    @Pattern(regexp = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$", message = "{" + MessageKeys.INVALID_EMAIL + "}")
    private String email;

    @Schema(description = "Số điện thoại", example = "0901234567", required = true)
    @NotBlank(message = "{" + MessageKeys.NOT_BLANK_PHONE_NUMBER + "}")
    @Pattern(regexp = "\\d{10}", message = "{" + MessageKeys.PHONE_NUMBER_INVALID + "}")
    private String phoneNumber;

    @Schema(description = "Mật khẩu, ít nhất 8 ký tự, 1 chữ hoa, 1 ký tự đặc biệt", example = "Password123!", required = true)
    @NotBlank(message = "{" + MessageKeys.NOT_BLANK_PASSWORD + "}")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[!@#$%^&*])[A-Za-z0-9!@#$%^&*]{8,}$",
            message = "{" + MessageKeys.PASSWORD_PATTERN + "}"
    )
    private String password;

    @Schema(description = "Ngày sinh", example = "08/06/2008", required = true)
    @NotNull(message = "{" + MessageKeys.NOT_NULL_DOB + "}")
    @Past(message = "{" + MessageKeys.DOB_MUST_BE_PAST + "}")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    @Schema(description = "Giới tính", example = "male", required = true)
    @NotNull(message = "{" + MessageKeys.INVALID_GENDER + "}")
    private Gender gender;
}