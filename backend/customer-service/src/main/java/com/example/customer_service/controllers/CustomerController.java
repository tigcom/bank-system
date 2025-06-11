package com.example.customer_service.controllers;

import com.example.common_service.dto.AccountDTO;
import com.example.common_service.services.account.AccountQueryService;
import com.example.common_service.services.customer.CustomerCommonService;
import com.example.customer_service.dtos.*;
import com.example.customer_service.models.Customer;
import com.example.customer_service.models.KycProfile;
import com.example.customer_service.models.KycStatus;
import com.example.customer_service.repositories.CustomerRepository;
import com.example.customer_service.repositories.KycProfileRepository;
import com.example.customer_service.responses.*;
import com.example.customer_service.services.CustomerService;
import com.example.customer_service.ultils.MessageKeys;
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
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.rpc.RpcContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Tag(name = "Customer Controller", description = "Quản lý người dùng: đăng ký, đăng nhập, và KYC")
public class CustomerController {

    private static final Logger log = LoggerFactory.getLogger("ACCESS_LOG");

    private final CustomerService customerService;

    private final MessageSource messageSource;

    @DubboReference
    private CustomerCommonService customerCommonService;

    private final CustomerRepository customerRepository;
    private final KycProfileRepository kycProfileRepository;

    @Operation(summary = "Đăng ký người dùng", description = "Tạo tài khoản người dùng mới")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Đăng ký thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ")
    })
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterCustomerDTO request) {
        try {
            customerService.sentOtpRegister(request); // Gửi OTP trước
            log.info("Yêu cầu OTP đăng ký thành công cho email: {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ApiResponseWrapper<>(
                            HttpStatus.OK.value(),
                            getMessage(MessageKeys.OTP_SENT),
                            null));
        } catch (IllegalArgumentException e) {
            log.error("Yêu cầu OTP đăng ký thất bại cho email: {} - {}", request.getEmail(), e.getMessage());
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponseWrapper.error(e.getMessage()));
        }
    }

    @Operation(summary = "Xác nhận OTP đăng ký", description = "Xác nhận OTP để hoàn tất đăng ký")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Đăng ký thành công"),
            @ApiResponse(responseCode = "400", description = "OTP không hợp lệ"),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ")
    })
    @PostMapping("/confirm-register")
    public ResponseEntity<?> confirmRegister(
            @RequestParam String email,
            @RequestParam String otp) {
        try {
            ApiResponseWrapper<?> response = customerService.confirmRegister(email, otp);
            log.info("Xác nhận OTP thành công cho email: {}", email);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(response);
        } catch (IllegalArgumentException e) {
            log.error("Xác nhận OTP thất bại cho email: {} - {}", email, e.getMessage());
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponseWrapper.error(e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody @Valid ResetPasswordDTO request) {
        try {
            ApiResponseWrapper<?> response = customerService.resetPassword(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponseWrapper.error(e.getMessage()));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody @Valid ForgotPasswordDTO request) {
        try {
            customerService.sentEmailForgotPassword(request.getEmail());
            return ResponseEntity.ok(new ApiResponseWrapper<>(
                    HttpStatus.OK.value(),
                    getMessage(MessageKeys.FORGOT_PASSWORD_LINK_SENT),
                    new Response(true, getMessage(MessageKeys.FORGOT_PASSWORD_NOTIFICATION))));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponseWrapper.error(e.getMessage()));
        }
    }

    @Operation(summary = "Lấy danh sách khách hàng", description = "Truy vấn tất cả khách hàng")
    @ApiResponse(responseCode = "200", description = "Lấy danh sách khách hàng thành công",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CustomerListResponse.class)))
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/list")
    public ResponseEntity<?> getCustomerList() {
        try {
            log.info("Fetching customer list");
            CustomerListResponse response = customerService.getCustomerList();
            return ResponseEntity.ok(new ApiResponseWrapper<>(
                    HttpStatus.OK.value(),
                    getMessage(MessageKeys.SUCCESS_GET_CUSTOMER),
                    response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponseWrapper.error(e.getMessage()));
        }
    }

    @Operation(summary = "Lấy thông tin chi tiết khách hàng", description = "Truy vấn khách hàng khi đăng nhập")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy thông tin khách hàng thành công",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CustomerResponse.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy khách hàng")
    })
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    @GetMapping("/detail")
    public ResponseEntity<?> getCustomerDetail() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userID = authentication.getName();
            log.info("Fetching customer detail for userId: {}", userID);

            CustomerResponse customer = customerService.getCustomerDetail(userID);
            return ResponseEntity.ok(new ApiResponseWrapper<>(
                    HttpStatus.OK.value(),
                    getMessage(MessageKeys.SUCCESS_GET_CUSTOMER),
                    customer));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponseWrapper.error(e.getMessage()));
        }
    }

    @Operation(summary = "Lấy thông tin chi tiết khách hàng", description = "Truy vấn khách hàng theo cifCode")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy thông tin khách hàng thành công",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CustomerResponse.class))),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy khách hàng")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/detail/{cifCode}")
    public ResponseEntity<?> getCustomerDetailByCifCode(@PathVariable String cifCode) {
        try {
            log.info("Admin fetching customer detail for cifCode: {}", cifCode);
            CustomerResponse customer = customerService.getCustomerDetailByCifCode(cifCode);
            return ResponseEntity.ok(new ApiResponseWrapper<>(
                    HttpStatus.OK.value(),
                    getMessage(MessageKeys.SUCCESS_GET_CUSTOMER),
                    customer));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponseWrapper.error(e.getMessage()));
        }
    }

    @Operation(summary = "Cập nhật mật khẩu khách hàng", description = "Cập nhật mật khẩu hiện tại")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật mật khẩu thành công",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    })
    @PreAuthorize("hasRole('CUSTOMER')")
    @PutMapping("/update-password")
    public ResponseEntity<?> updatePassword(
            @Valid @RequestBody ChangePasswordDTO request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userID = authentication.getName();
            log.info("Update password request for customerId: {}", userID);
            ApiResponseWrapper<?> response = customerService.updateCustomerPassword(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponseWrapper.error(e.getMessage()));
        }
    }

    @Operation(summary = "Cập nhật thông tin khách hàng", description = "Cập nhật thông tin cá nhân khách hàng")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật thành công",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    })
    @PreAuthorize("hasRole('CUSTOMER')")
    @PutMapping("/update")
    public ResponseEntity<?> updateCustomer(@Valid @RequestBody UpdateCustomerDTO request) {
        try {
            log.info("Update customer request for name: {}", request.getFullName());
            ApiResponseWrapper<?> response = customerService.updateCustomer(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponseWrapper.error(e.getMessage()));
        }
    }

    @Operation(summary = "Cập nhật trạng thái khách hàng", description = "Cập nhật trạng thái active, suspended,...")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật trạng thái thành công",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Response.class))),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/status")
    public ResponseEntity<?> updateCustomerStatus(@Valid @RequestBody UpdateStatusRequest request) {
        try {
            log.info("Update status request for customerId: {}", request.getCifCode());
            ApiResponseWrapper<?> response = customerService.updateCustomerStatus(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponseWrapper.error(e.getMessage()));
        }
    }

    @Operation(summary = "Kiểm tra trạng thái KYC", description = "Trả về trạng thái xác minh KYC của người dùng")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy trạng thái KYC thành công",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = KycResponse.class))),
            @ApiResponse(responseCode = "400", description = "Không thể lấy trạng thái KYC")
    })
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/status")
    public ResponseEntity<?> checkKycStatus() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userId = authentication.getName();

            log.info("Kiểm tra trạng thái KYC cho userId: {}", userId);
            Customer customer = customerRepository.findCustomerByUserId(userId);

            if (customer == null) {
                return ResponseEntity
                        .badRequest()
                        .body(ApiResponseWrapper.error("Không tìm thấy người dùng"));
            }

            Optional<KycProfile> kycProfileOpt = kycProfileRepository.findByCustomer(customer);
            boolean isKycVerified = kycProfileOpt.isPresent() && KycStatus.VERIFIED.equals(kycProfileOpt.get().getStatus());

            System.out.println(isKycVerified);

            KycResponse response = new KycResponse(
                    isKycVerified,
                    isKycVerified ? "Tài khoản đã được xác minh KYC" : "Tài khoản chưa được xác minh KYC",
                    null
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Kiểm tra trạng thái KYC thất bại, Lỗi: {}", e.getMessage());
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponseWrapper.error(e.getMessage()));
        }
    }

    @Operation(summary = "Xác minh KYC", description = "Xác minh thông tin khách hàng KYC")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Xác minh thành công",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = KycResponse.class))),
            @ApiResponse(responseCode = "400", description = "Xác minh thất bại")
    })
    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/kyc/verify")
    public ResponseEntity<?> verifyKyc(@Valid @RequestBody KycRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userId = authentication.getName();

            log.info("KYC verification request for userId: {}", userId);
            KycResponse response = customerService.verifyKyc(userId, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponseWrapper.error(e.getMessage()));
        }
    }

    @GetMapping("/accounts")
    public ResponseEntity<?> getCustomerAccounts() {
        try {
            log.info("Start get list account");
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userID = authentication.getName();
            Customer customer = customerRepository.findCustomerByUserId(userID);
            log.info("Get list account for username: {}", customer.getUsername());

            SecurityContextHolder.clearContext();

            List<AccountDTO> accounts = customerCommonService.getAccountsByCifCode(customer.getCifCode());
            log.info("Get list account successfully");
            return ResponseEntity.ok(accounts);
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponseWrapper.error(e.getMessage()));
        }
    }


    private String getMessage(String key, Object... args) {
        return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
    }
}