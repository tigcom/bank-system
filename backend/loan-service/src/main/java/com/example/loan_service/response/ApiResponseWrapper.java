package com.example.loan_service.response;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponseWrapper<T> {
    private int status;
    private String message;
    private T data;
}