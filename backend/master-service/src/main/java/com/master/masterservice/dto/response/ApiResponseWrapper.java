package com.master.masterservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponseWrapper<T> {
    private T data;
    private String message;
    private String status;
    private String errorCode;
    
    public static <T> ApiResponseWrapper<T> success(T data) {
        return ApiResponseWrapper.<T>builder()
                .data(data)
                .status("SUCCESS")
                .build();
    }
    
    public static <T> ApiResponseWrapper<T> error(String message, String errorCode) {
        return ApiResponseWrapper.<T>builder()
                .message(message)
                .status("ERROR")
                .errorCode(errorCode)
                .build();
    }
}