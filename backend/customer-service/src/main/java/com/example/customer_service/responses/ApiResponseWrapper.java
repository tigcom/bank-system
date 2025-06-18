package com.example.customer_service.responses;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.http.HttpStatus;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponseWrapper<T> {
    private int status;
    private String message;
    private T data;

    public static <T> ApiResponseWrapper<T> success(String message, T data) {
        return new ApiResponseWrapper<>(HttpStatus.OK.value(), message, data);
    }

    public static <T> ApiResponseWrapper<T> error(String message) {
        return new ApiResponseWrapper<>(HttpStatus.BAD_REQUEST.value(), message, null);
    }
}

