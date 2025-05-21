package com.example.customer_service.responses;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Thông tin phản hồi lỗi")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {

    @Schema(description = "Mã lỗi", example = "400")
    private int status;

    @Schema(description = "Thông báo lỗi", example = "Password cannot be blank")
    private String message;

    @Schema(description = "API path gây ra lỗi", example = "/api/users/login")
    private String path;

}
