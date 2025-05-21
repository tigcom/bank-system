package com.example.customer_service.controllers;

import com.example.customer_service.dtos.*;
import com.example.customer_service.responses.*;
import com.example.customer_service.services.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponseWrapper<RegisterResponse>> register(@RequestBody RegisterCustomerDTO request) {
        RegisterResponse response = customerService.register(request);
        return ResponseEntity.status(response.isSuccess() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST)
                .body(new ApiResponseWrapper<>(
                        response.isSuccess() ? HttpStatus.CREATED.value() : HttpStatus.BAD_REQUEST.value(),
                        response.getMessage(),
                        response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponseWrapper<LoginResponse>> login(@RequestBody LoginCustomerDTO request) {
        LoginResponse response = customerService.login(request);
        return ResponseEntity.status(response.getToken() != null ? HttpStatus.OK : HttpStatus.UNAUTHORIZED)
                .body(new ApiResponseWrapper<>(
                        response.getToken() != null ? HttpStatus.OK.value() : HttpStatus.UNAUTHORIZED.value(),
                        response.getToken() != null ? "Login successful" : "Invalid credentials",
                        response));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponseWrapper<String>> forgotPassword(@RequestBody String email) {
        RegisterResponse response = customerService.forgotPassword(email);
        return ResponseEntity.status(response.isSuccess() ? HttpStatus.OK : HttpStatus.NOT_FOUND)
                .body(new ApiResponseWrapper<>(
                        response.isSuccess() ? HttpStatus.OK.value() : HttpStatus.NOT_FOUND.value(),
                        response.getMessage(),
                        null));
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponseWrapper<CustomerListResponse>> getCustomerList() {
        CustomerListResponse response = customerService.getCustomerList();
        return ResponseEntity.ok(new ApiResponseWrapper<>(
                HttpStatus.OK.value(),
                "Customers retrieved successfully",
                response));
    }

    @GetMapping("/detail")
    public ResponseEntity<ApiResponseWrapper<CustomerResponse>> getCustomerDetail(
            @RequestParam(required = false) String cifCode) {
        CustomerResponse customer = customerService.getCustomerDetail(cifCode);
        return ResponseEntity.status(customer != null ? HttpStatus.OK : HttpStatus.NOT_FOUND)
                .body(new ApiResponseWrapper<>(
                        customer != null ? HttpStatus.OK.value() : HttpStatus.NOT_FOUND.value(),
                        customer != null ? "Customer retrieved successfully" : "Customer not found",
                        customer));
    }

    @PutMapping("/update")
    public ResponseEntity<ApiResponseWrapper<RegisterResponse>> updateCustomer(@RequestBody UpdateCustomerDTO request) {
        RegisterResponse response = customerService.updateCustomer(request);
        return ResponseEntity.status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(new ApiResponseWrapper<>(
                        response.isSuccess() ? HttpStatus.OK.value() : HttpStatus.BAD_REQUEST.value(),
                        response.getMessage(),
                        response));
    }

    @PutMapping("/status")
    public ResponseEntity<ApiResponseWrapper<RegisterResponse>> updateCustomerStatus(@RequestBody UpdateStatusRequest request) {
        RegisterResponse response = customerService.updateCustomerStatus(request);
        return ResponseEntity.status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(new ApiResponseWrapper<>(
                        response.isSuccess() ? HttpStatus.OK.value() : HttpStatus.BAD_REQUEST.value(),
                        response.getMessage(),
                        response));
    }

    @PostMapping("/kyc/verify")
    public ResponseEntity<ApiResponseWrapper<KycResponse>> verifyKyc(@RequestBody KycRequest request) {
        KycResponse response = customerService.verifyKyc(request);
        return ResponseEntity.status(response.isVerified() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(new ApiResponseWrapper<>(
                        response.isVerified() ? HttpStatus.OK.value() : HttpStatus.BAD_REQUEST.value(),
                        response.getMessage(),
                        response));
    }
}