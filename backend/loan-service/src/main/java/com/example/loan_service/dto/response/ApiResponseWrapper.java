package com.example.loan_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponseWrapper<T> {
    private int status;

    private String message;

    private T data;
}