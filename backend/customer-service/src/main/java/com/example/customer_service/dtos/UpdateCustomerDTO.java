package com.example.customer_service.dtos;

import com.example.customer_service.ultils.MessageKeys;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "DTO cập nhật thông tin khách hàng")
public class UpdateCustomerDTO {

    @Schema(description = "Họ tên khách hàng", example = "Nguyễn Văn A", required = true)
    @NotBlank(message = "{" + MessageKeys.NOT_BLANK_FULL_NAME + "}")
    private String fullName;

    @Schema(description = "Địa chỉ khách hàng", example = "123 Đường ABC, Quận 1", required = true)
    @NotBlank(message = "{" + MessageKeys.NOT_BLANK_ADDRESS+ "}")
    private String address;

    @Schema(description = "Số điện thoại", example = "0901234567", required = true)
    @NotBlank(message = "{" + MessageKeys.NOT_BLANK_PHONE_NUMBER + "}")
    @Pattern(regexp = "\\d{10}", message = "{" + MessageKeys.PHONE_NUMBER_INVALID + "}")
    private String phoneNumber;
}
