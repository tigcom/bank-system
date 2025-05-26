package com.example.customer_service.dtos;

import com.example.customer_service.models.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

import jakarta.validation.constraints.*;

@Data
@Schema(description = "Thông tin đăng ký khách hàng")
public class RegisterCustomerDTO {

    @Schema(description = "Tên đăng nhập", example = "nguyenvana123", required = true)
    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Size(min = 3, max = 50, message = "Tên đăng nhập phải từ 3 đến 50 ký tự")
    @Pattern(regexp = "^[a-zA-Z0-9._-]{3,50}$", message = "Tên đăng nhập chỉ được chứa chữ cái, số, dấu chấm, gạch dưới hoặc gạch ngang")
    private String username;

    @Schema(description = "Họ tên khách hàng", example = "Nguyễn Văn A", required = true)
    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    @Schema(description = "Địa chỉ khách hàng", example = "123 Đường ABC, Quận 1", required = true)
    @NotBlank(message = "Địa chỉ không được để trống")
    private String address;

    @Schema(description = "Số CMND/CCCD", example = "012345678", required = true)
    @NotBlank(message = "Số CMND/CCCD không được để trống")
    private String identityNumber;

    @Schema(description = "Email", example = "example@gmail.com", required = true)
    @Email(message = "Email không hợp lệ")
    @NotBlank(message = "Email không được để trống")
    private String email;

    @Schema(description = "Số điện thoại", example = "0901234567", required = true)
    @NotBlank(message = "Số điện thoại không được để trống")
    private String phoneNumber;

    @Schema(description = "Mật khẩu", required = true)
    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;

    @Schema(description = "Ngày sinh", example = "1990-01-01", required = true)
    @NotNull(message = "Ngày sinh không được để trống")
    @Past(message = "Ngày sinh phải trong quá khứ")
    private LocalDate dateOfBirth;

    @Schema(description = "Giới tính", example = "male", required = true)
    @NotNull(message = "Giới tính không được để trống")
    private Gender gender;

}
