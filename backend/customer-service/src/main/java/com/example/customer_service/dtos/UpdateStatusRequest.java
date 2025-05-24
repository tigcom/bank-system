package com.example.customer_service.dtos;

import com.example.customer_service.models.CustomerStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "DTO cập nhật trạng thái khách hàng")
public class UpdateStatusRequest {

    @Schema(description = "ID khách hàng", example = "123", required = true)
    @NotNull(message = "ID không được để trống")
    private Long id;

    @Schema(description = "Trạng thái khách hàng", example = "ACTIVE", required = true)
    @NotNull(message = "Trạng thái không được để trống")
    private CustomerStatus status;
}
