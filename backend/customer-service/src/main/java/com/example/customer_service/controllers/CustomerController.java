package com.example.customer_service.controllers;

import com.example.customer_service.dtos.*;
import com.example.customer_service.responses.*;
import com.example.customer_service.services.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponseWrapper<RegisterResponse>> register(@RequestBody RegisterRequest request) {
        RegisterResponse response = customerService.register(request);
        return ResponseEntity.status(response.isSuccess() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST)
                .body(new ApiResponseWrapper<>(
                        response.isSuccess() ? HttpStatus.CREATED.value() : HttpStatus.BAD_REQUEST.value(),
                        response.getMessage(),
                        response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponseWrapper<LoginResponse>> login(@RequestBody LoginRequest request) {
        LoginResponse response = customerService.login(request);
        return ResponseEntity.status(response.getToken() != null ? HttpStatus.OK : HttpStatus.UNAUTHORIZED)
                .body(new ApiResponseWrapper<>(
                        response.getToken() != null ? HttpStatus.OK.value() : HttpStatus.UNAUTHORIZED.value(),
                        response.getToken() != null ? "Login successful" : "Invalid credentials",
                        response));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponseWrapper<ForgotPasswordResponse>> forgotPassword(@RequestBody String email) {
        ForgotPasswordResponse response = customerService.forgotPassword(email);
        return ResponseEntity.status(response.isSuccess() ? HttpStatus.OK : HttpStatus.NOT_FOUND)
                .body(new ApiResponseWrapper<>(
                        response.isSuccess() ? HttpStatus.OK.value() : HttpStatus.NOT_FOUND.value(),
                        response.getMessage(),
                        response));
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponseWrapper<List<CustomerDTO>>> getCustomerList() {
        List<CustomerDTO> customers = customerService.getCustomerList();
        return ResponseEntity.ok(new ApiResponseWrapper<>(
                HttpStatus.OK.value(),
                "Customers retrieved successfully",
                customers));
    }

    @GetMapping("/detail")
    public ResponseEntity<ApiResponseWrapper<CustomerDTO>> getCustomerDetail(
            @RequestParam(required = false) String cifCode) {
        CustomerDTO customerDTO = customerService.getCustomerDetail(cifCode);
        return ResponseEntity.status(customerDTO != null ? HttpStatus.OK : HttpStatus.NOT_FOUND)
                .body(new ApiResponseWrapper<>(
                        customerDTO != null ? HttpStatus.OK.value() : HttpStatus.NOT_FOUND.value(),
                        customerDTO != null ? "Customer retrieved successfully" : "Customer not found",
                        customerDTO));
    }

    @PutMapping("/update")
    public ResponseEntity<ApiResponseWrapper<UpdateCustomerResponse>> updateCustomer(@RequestBody UpdateCustomerRequest request) {
        UpdateCustomerResponse response = customerService.updateCustomer(request);
        return ResponseEntity.status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(new ApiResponseWrapper<>(
                        response.isSuccess() ? HttpStatus.OK.value() : HttpStatus.BAD_REQUEST.value(),
                        response.getMessage(),
                        response));
    }

    @PutMapping("/status")
    public ResponseEntity<ApiResponseWrapper<UpdateStatusResponse>> updateCustomerStatus(@RequestBody UpdateStatusRequest request) {
        UpdateStatusResponse response = customerService.updateCustomerStatus(request);
        return ResponseEntity.status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(new ApiResponseWrapper<>(
                        response.isSuccess() ? HttpStatus.OK.value() : HttpStatus.BAD_REQUEST.value(),
                        response.getMessage(),
                        response));
    }
}