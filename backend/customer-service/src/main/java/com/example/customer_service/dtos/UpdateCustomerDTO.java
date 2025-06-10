package com.example.customer_service.dtos;

import com.example.customer_service.models.Gender;
import com.example.customer_service.ultils.MessageKeys;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "DTO cập nhật thông tin khách hàng")
public class UpdateCustomerDTO {

    @Schema(description = "Họ tên khách hàng", example = "Nguyễn Văn A", required = true)
    @NotBlank(message = "{" + MessageKeys.NOT_BLANK_FULL_NAME + "}")
    private String fullName;

    @Schema(description = "Ngày sinh", example = "08/06/2008", required = true)
    @NotNull(message = "{" + MessageKeys.NOT_NULL_DOB + "}")
    @Past(message = "{" + MessageKeys.DOB_MUST_BE_PAST + "}")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    @Schema(description = "Địa chỉ khách hàng", example = "123 Đường ABC, Quận 1", required = true)
    @NotBlank(message = "{" + MessageKeys.NOT_BLANK_ADDRESS+ "}")
    private String address;

    @Schema(description = "Email", example = "john.doe@example.com", required = true)
    @NotBlank(message = "{" + MessageKeys.NOT_BLANK_EMAIL + "}")
    @Pattern(regexp = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$", message = "{" + MessageKeys.INVALID_EMAIL + "}")
    private String email;

    @Schema(description = "Số điện thoại", example = "0901234567", required = true)
    @NotBlank(message = "{" + MessageKeys.NOT_BLANK_PHONE_NUMBER + "}")
    @Pattern(regexp = "\\d{10}", message = "{" + MessageKeys.PHONE_NUMBER_INVALID + "}")
    private String phoneNumber;

    @Schema(description = "Giới tính", example = "male", required = true)
    @NotNull(message = "{" + MessageKeys.INVALID_GENDER + "}")
    private Gender gender;
}
