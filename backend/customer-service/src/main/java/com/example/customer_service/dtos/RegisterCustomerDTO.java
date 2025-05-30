package com.example.customer_service.dtos;

import com.example.customer_service.models.Gender;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.joda.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.joda.ser.LocalDateSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@Schema(description = "Thông tin đăng ký khách hàng")
public class RegisterCustomerDTO implements Serializable {

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
    @Pattern(regexp = "\\d{9}|\\d{12}", message = "Số CMND/CCCD phải có 9 hoặc 12 chữ số")
    private String identityNumber;

    @Schema(description = "Email", example = "john.doe@example.com", required = true)
    @NotBlank(message = "Email không được để trống")
    @Pattern(regexp = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$", message = "Email không hợp lệ")
    private String email;

    @Schema(description = "Số điện thoại", example = "0901234567", required = true)
    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "\\d{10}", message = "Số điện thoại phải có 10 chữ số")
    private String phoneNumber;

    @Schema(description = "Mật khẩu, ít nhất 8 ký tự, 1 chữ hoa, 1 ký tự đặc biệt", example = "Password123!", required = true)
    @NotBlank(message = "Mật khẩu không được để trống")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[!@#$%^&*])[A-Za-z0-9!@#$%^&*]{8,}$",
            message = "Mật khẩu phải có ít nhất 8 ký tự, bao gồm 1 chữ hoa và 1 ký tự đặc biệt (!@#$%^&*)"
    )
    private String password;

    @Schema(description = "Ngày sinh", example = "1990-01-01", required = true)
    @NotNull(message = "Ngày sinh không được để trống")
    @Past(message = "Ngày sinh phải trong quá khứ")
    private LocalDate dateOfBirth;

    @Schema(description = "Giới tính", example = "male", required = true)
    @NotNull(message = "Giới tính không được để trống")
    private Gender gender;
}