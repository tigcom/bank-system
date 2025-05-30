package com.example.corebanking_service.dto.resonse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ApiResponseWrapper<T> implements Serializable {

    private int status;
    private String message;
    private T data;


    public static <T> ApiResponseWrapper<T> success(int status,String message, T data) {
        return new ApiResponseWrapper<>(status, message, data);
    }

    public static <T> ApiResponseWrapper<T> error(int status, String message) {
        return new ApiResponseWrapper<>(status, message, null);
    }


}