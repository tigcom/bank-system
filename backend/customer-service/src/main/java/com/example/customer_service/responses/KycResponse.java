package com.example.customer_service.responses;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO phản hồi KYC")
public class KycResponse {

    @Schema(description = "Trạng thái xác minh", example = "true")
    private boolean verified;

    @Schema(description = "Thông điệp xác minh", example = "Xác minh thành công")
    private String message;

    @Schema(description = "Chi tiết thêm (nếu có)", example = "Không có lỗi")
    private String details;
}
