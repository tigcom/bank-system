package com.example.transaction_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Kết quả trả về chung cho tất cả API")
public class ApiResponse<T> {

    @Builder.Default
    @Schema(description = "Mã kết quả", example = "200")
    private int code = 1000;

    @Schema(description = "Thông điệp trả về", example = "Thành công")
    private String message;

    @Schema(description = "Dữ liệu kết quả trả về")
    private T result;
}
