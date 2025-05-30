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
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Tag(name = "Customer Controller", description = "Quản lý người dùng: đăng ký, đăng nhập, và KYC")
@Slf4j
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginCustomerDTO request) throws Exception {
        try {
            Response response = customerService.login(request);
            log.info("Đăng nhập thành công cho username: {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponseWrapper<>(
                            HttpStatus.OK.value(),
                            response.getMessage(),
                            request.getUsername()));
        } catch (IllegalArgumentException e) {
            log.error("Đăng nhập thất bại cho username: {} - {}", request.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseWrapper<>(
                            HttpStatus.BAD_REQUEST.value(),
                            e.getMessage(),
                            null));
        }
    }


    @Operation(summary = "Đăng ký người dùng", description = "Tạo tài khoản người dùng mới")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Đăng ký thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ")
    })
    @PostMapping("/register")
    public ResponseEntity<ApiResponseWrapper<Response>> register(@Valid @RequestBody RegisterCustomerDTO request) {
        try {
            customerService.sentOtpRegister(request); // Gửi OTP trước
            log.info("Yêu cầu OTP đăng ký thành công cho email: {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ApiResponseWrapper<>(
                            HttpStatus.OK.value(),
                            "OTP đã được gửi tới email của bạn",
                            null));
        } catch (IllegalArgumentException e) {
            log.error("Yêu cầu OTP đăng ký thất bại cho email: {} - {}", request.getEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseWrapper<>(
                            HttpStatus.BAD_REQUEST.value(),
                            e.getMessage(),
                            null));
        }
    }

    @Operation(summary = "Xác nhận OTP đăng ký", description = "Xác nhận OTP để hoàn tất đăng ký")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Đăng ký thành công"),
            @ApiResponse(responseCode = "400", description = "OTP không hợp lệ"),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ")
    })
    @PostMapping("/confirm-register")
    public ResponseEntity<ApiResponseWrapper<Response>> confirmRegister(
            @RequestParam String email,
            @RequestParam String otp) {
        try {
            Response response = customerService.confirmRegister(email, otp);
            log.info("Xác nhận OTP thành công cho email: {}", email);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponseWrapper<>(
                            HttpStatus.CREATED.value(),
                            response.getMessage(),
                            response));
        } catch (IllegalArgumentException e) {
            log.error("Xác nhận OTP thất bại cho email: {} - {}", email, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseWrapper<>(
                            HttpStatus.BAD_REQUEST.value(),
                            e.getMessage(),
                            null));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponseWrapper<Response>> resetPassword(@Valid @RequestParam String token , @RequestBody ResetPasswordDTO request) {
        try {
            Response response = customerService.resetPassword(token, request);
            return ResponseEntity.ok(new ApiResponseWrapper<>(
                    HttpStatus.OK.value(),
                    response.getMessage(),
                    response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponseWrapper<>(
                    HttpStatus.BAD_REQUEST.value(),
                    e.getMessage(),
                    null));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponseWrapper<Response>> forgotPassword(@RequestBody @Valid ForgotPasswordDTO request) {
        try {
            customerService.sentEmailForgotPassword(request.getEmail());
            return ResponseEntity.ok(new ApiResponseWrapper<>(
                    HttpStatus.OK.value(),
                    "Email khôi phục mật khẩu đã được gửi",
                    new Response(true, "Vui lòng kiểm tra email của bạn")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseWrapper<>(
                    HttpStatus.BAD_REQUEST.value(),
                    e.getMessage(),
                    null));
        }
    }

//    @Operation(
//            summary = "Yêu cầu khôi phục mật khẩu",
//            description = "Gửi email chứa liên kết để khôi phục mật khẩu"
//    )
//    @ApiResponses({
//            @ApiResponse(responseCode = "200", description = "Liên kết khôi phục mật khẩu đã được gửi"),
//            @ApiResponse(responseCode = "400", description = "Email không tồn tại hoặc không hợp lệ"),
//            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ")
//    })
//    @PostMapping("/forgot-password")
//    public ResponseEntity<ApiResponseWrapper<Response>> forgotPassword(@RequestBody @Valid ForgotPasswordDTO request) {
//        try {
//            Response response = customerService.forgotPassword(email);
//            log.info("Yêu cầu khôi phục mật khẩu thành công cho email: {}", email);
//            return ResponseEntity.status(HttpStatus.OK)
//                    .body(new ApiResponseWrapper<>(
//                            HttpStatus.OK.value(),
//                            response.getMessage(),
//                            response
//                    ));
//        } catch (IllegalArgumentException | EntityNotFoundException e) {
//            log.error("Yêu cầu khôi phục mật khẩu thất bại cho email: {} - {}", email, e.getMessage());
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                    .body(new ApiResponseWrapper<>(
//                            HttpStatus.BAD_REQUEST.value(),
//                            e.getMessage(),
//                            null
//                    ));
//        } catch (Exception e) {
//            log.error("Lỗi không xác định khi xử lý quên mật khẩu cho email: {}", email, e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(new ApiResponseWrapper<>(
//                            HttpStatus.INTERNAL_SERVER_ERROR.value(),
//                            "Lỗi máy chủ khi xử lý yêu cầu",
//                            null
//                    ));
//        }
//    }


    @Operation(summary = "Lấy danh sách khách hàng", description = "Truy vấn tất cả khách hàng")
    @ApiResponse(responseCode = "200", description = "Lấy danh sách khách hàng thành công",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CustomerListResponse.class)))
    @PreAuthorize("hasRole('CUSTOMER')")
//    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/detail")
    public ResponseEntity<ApiResponseWrapper<CustomerResponse>> getCustomerDetail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userID = authentication.getName();
        log.info("Fetching customer detail for userId: {}", userID);

        CustomerResponse customer = customerService.getCustomerDetail();
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
    @PreAuthorize("hasRole('CUSTOMER')")
    @PutMapping("/update-password")
    public ResponseEntity<ApiResponseWrapper<Response>> updatePassword(
            @Valid @RequestBody ChangePasswordDTO request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userID = authentication.getName();
        log.info("Update password request for customerId: {}", userID);
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
    @PreAuthorize("hasRole('CUSTOMER')")
    @PutMapping("/update")
    public ResponseEntity<ApiResponseWrapper<Response>> updateCustomer(@Valid @RequestBody UpdateCustomerDTO request) {
        log.info("Update customer request for name: {}", request.getFullName());
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
    @PreAuthorize("hasRole('CUSTOMER')")
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