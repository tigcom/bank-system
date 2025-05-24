package com.example.customer_service.controllers;

import com.example.customer_service.dtos.*;
import com.example.customer_service.responses.*;
import com.example.customer_service.services.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Tag(name = "Customer Controller", description = "Quản lý người dùng: đăng ký, đăng nhập, và KYC")
@Slf4j
public class CustomerController {

    private final CustomerService customerService;

    @Operation(summary = "Đăng ký người dùng", description = "Tạo tài khoản người dùng mới")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Đăng ký thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    })
    @PostMapping("/register")
    public ResponseEntity<ApiResponseWrapper<Response>> register(@Valid @RequestBody RegisterCustomerDTO request) {
        try {
            Response response = customerService.register(request);
            log.info("Register request for username: {}", request.getUsername());
            return ResponseEntity.status(response.isSuccess() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseWrapper<>(
                            response.isSuccess() ? HttpStatus.CREATED.value() : HttpStatus.BAD_REQUEST.value(),
                            response.getMessage(),
                            response));
        } catch (Exception e) {
            log.error("Registration failed for username: {} - {}", request.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseWrapper<>(
                    HttpStatus.BAD_REQUEST.value(),
                    "Registration failed: " + e.getMessage(),
                    null));
        }
    }

    @Operation(summary = "Quên mật khẩu", description = "Gửi yêu cầu lấy lại mật khẩu qua email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Yêu cầu lấy lại mật khẩu thành công"),
            @ApiResponse(responseCode = "404", description = "Email không tồn tại")
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponseWrapper<Response>> forgotPassword(@Valid @RequestBody ForgotPasswordDTO request) {
        log.info("Forgot password request for email: {}", request.getEmail());
        Response response = customerService.forgotPassword(request.getEmail());
        return ResponseEntity.status(response.isSuccess() ? HttpStatus.OK : HttpStatus.NOT_FOUND)
                .body(new ApiResponseWrapper<>(
                        response.isSuccess() ? HttpStatus.OK.value() : HttpStatus.NOT_FOUND.value(),
                        response.getMessage(),
                        response));
    }


    @Operation(summary = "Lấy danh sách khách hàng", description = "Truy vấn tất cả khách hàng")
    @ApiResponse(responseCode = "200", description = "Lấy danh sách khách hàng thành công",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CustomerListResponse.class)))
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/list")
    public ResponseEntity<ApiResponseWrapper<CustomerListResponse>> getCustomerList() {
        log.info("Fetching customer list");
        CustomerListResponse response = customerService.getCustomerList();
        return ResponseEntity.ok(new ApiResponseWrapper<>(
                HttpStatus.OK.value(),
                "Customers retrieved successfully",
                response));
    }

    @Operation(summary = "Lấy thông tin chi tiết khách hàng", description = "Truy vấn khách hàng theo cifCode")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy thông tin khách hàng thành công",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CustomerResponse.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy khách hàng")
    })
    @GetMapping("/detail")
    public ResponseEntity<ApiResponseWrapper<CustomerResponse>> getCustomerDetail(
            @RequestParam String cifCode) {
        log.info("Fetching customer detail for cifCode: {}", cifCode);
        CustomerResponse customer = customerService.getCustomerDetail(cifCode);
        return ResponseEntity.status(customer != null ? HttpStatus.OK : HttpStatus.NOT_FOUND)
                .body(new ApiResponseWrapper<>(
                        customer != null ? HttpStatus.OK.value() : HttpStatus.NOT_FOUND.value(),
                        customer != null ? "Customer retrieved successfully" : "Customer not found",
                        customer));
    }

    @Operation(summary = "Cập nhật mật khẩu khách hàng", description = "Cập nhật mật khẩu hiện tại")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật mật khẩu thành công",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    })
    @PutMapping("/update-password")
    public ResponseEntity<ApiResponseWrapper<Response>> updatePassword(
            @Valid @RequestBody ChangePasswordDTO request) {
        log.info("Update password request for customerId: {}", request.getCustomerId());
        Response response = customerService.updateCustomerPassword(request);
        return ResponseEntity.status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(new ApiResponseWrapper<>(
                        response.isSuccess() ? HttpStatus.OK.value() : HttpStatus.BAD_REQUEST.value(),
                        response.getMessage(),
                        response));
    }

    @Operation(summary = "Cập nhật thông tin khách hàng", description = "Cập nhật thông tin cá nhân khách hàng")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật thành công",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    })
    @PutMapping("/update")
    public ResponseEntity<ApiResponseWrapper<Response>> updateCustomer(@Valid @RequestBody UpdateCustomerDTO request) {
        log.info("Update customer request for customerId: {}", request.getId());
        Response response = customerService.updateCustomer(request);
        return ResponseEntity.status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(new ApiResponseWrapper<>(
                        response.isSuccess() ? HttpStatus.OK.value() : HttpStatus.BAD_REQUEST.value(),
                        response.getMessage(),
                        response));
    }

    @Operation(summary = "Cập nhật trạng thái khách hàng", description = "Cập nhật trạng thái active, suspended,...")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật trạng thái thành công",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/status")
    public ResponseEntity<ApiResponseWrapper<Response>> updateCustomerStatus(@Valid @RequestBody UpdateStatusRequest request) {
        log.info("Update status request for customerId: {}", request.getId());
        Response response = customerService.updateCustomerStatus(request);
        return ResponseEntity.status(response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(new ApiResponseWrapper<>(
                        response.isSuccess() ? HttpStatus.OK.value() : HttpStatus.BAD_REQUEST.value(),
                        response.getMessage(),
                        response));
    }

    @Operation(summary = "Xác minh KYC", description = "Xác minh thông tin khách hàng KYC")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Xác minh thành công",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = KycResponse.class))),
            @ApiResponse(responseCode = "400", description = "Xác minh thất bại")
    })
    @PostMapping("/kyc/verify")
    public ResponseEntity<ApiResponseWrapper<KycResponse>> verifyKyc(@Valid @RequestBody KycRequest request) {
        log.info("KYC verification request for customerId: {}", request.getCustomerId());
        KycResponse response = customerService.verifyKyc(request);
        return ResponseEntity.status(response.isVerified() ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .body(new ApiResponseWrapper<>(
                        response.isVerified() ? HttpStatus.OK.value() : HttpStatus.BAD_REQUEST.value(),
                        response.getMessage(),
                        response));
    }
}