package com.example.customer_service.controllers;

import com.example.common_service.dto.AccountDTO;
import com.example.common_service.services.customer.CustomerCommonService;
import com.example.customer_service.dtos.*;
import com.example.customer_service.models.Customer;
import com.example.customer_service.repositories.CustomerRepository;
import com.example.customer_service.repositories.KycProfileRepository;
import com.example.customer_service.responses.*;
import com.example.customer_service.services.CustomerService;
import com.example.customer_service.services.KycService;
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
import org.apache.dubbo.config.annotation.DubboReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Tag(name = "Customer Controller", description = "Quản lý người dùng: đăng ký, đăng nhập, và KYC")
public class CustomerController {

    private static final Logger log = LoggerFactory.getLogger(CustomerController.class);

    private final CustomerService customerService;

    private final KycService kycService;

    private final MessageSource messageSource;

    @DubboReference
    private CustomerCommonService customerCommonService;

    private final CustomerRepository customerRepository;
    private final KycProfileRepository kycProfileRepository;

    @PostMapping("/register/initiate")
    @Operation(summary = "Bước 1: Khởi tạo đăng ký",
            description = "Lưu thông tin đăng ký vào bộ nhớ đệm để xác minh KYC")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Khởi tạo đăng ký thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ")
    })
    public ResponseEntity<ApiResponseWrapper<?>> khoiTaoDangKy(
            @Valid @RequestBody RegisterCustomerDTO request) {
        String requestId = UUID.randomUUID().toString();
        log.info("[khoiTaoDangKy] INITIATE_REGISTER_REQUEST - RequestId: {}, Email: {}, Username: {}", requestId, request.getEmail(), request.getUsername());
        try {
            ApiResponseWrapper<?> response = customerService.initiateRegister(request);
            log.info("[khoiTaoDangKy] INITIATE_REGISTER_SUCCESS - RequestId: {}, Email: {}, Username: {}", requestId, request.getEmail(), request.getUsername());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("[khoiTaoDangKy] INITIATE_REGISTER_FAILED - RequestId: {}, Email: {}, Username: {}, Error: {}", requestId, request.getEmail(), request.getUsername(), e.getMessage());
            return ResponseEntity.badRequest().body(
                    new ApiResponseWrapper<>(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null)
            );
        }
    }

    @PostMapping("/register/kyc-and-otp")
    @Operation(summary = "Bước 2: Xác minh KYC & gửi OTP",
            description = "Xác minh thông tin KYC và gửi mã OTP nếu thành công")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Xác minh KYC thành công, đã gửi OTP"),
            @ApiResponse(responseCode = "400", description = "Thông tin KYC không hợp lệ"),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ")
    })
    public ResponseEntity<ApiResponseWrapper<?>> processKycAndSendOtp(
            @RequestParam String email,
            @Valid @RequestBody KycRequest kycRequest) {
        String requestId = UUID.randomUUID().toString();
        log.info("[processKycAndSendOtp] KYC_AND_OTP_REQUEST - RequestId: {}, Email: {}, IdentityNumber: {}", requestId, email, kycRequest.getIdentityNumber());
        try {
            ApiResponseWrapper<?> response = customerService.processKycAndSendOtp(email, kycRequest);
            log.info("[processKycAndSendOtp] KYC_AND_OTP_SUCCESS - RequestId: {}, Email: {}", requestId, email);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("[processKycAndSendOtp] KYC_AND_OTP_FAILED - RequestId: {}, Email: {}, IdentityNumber: {}, Error: {}", requestId, email, kycRequest.getIdentityNumber(), e.getMessage());
            return ResponseEntity.badRequest().body(
                    new ApiResponseWrapper<>(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null)
            );
        }
    }

    @PostMapping("/register/send-otp")
    @Operation(summary = "Gửi lại OTP",
            description = "Gửi lại mã OTP cho người dùng")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đã gửi lại OTP"),
            @ApiResponse(responseCode = "400", description = "Gửi lại mã OTP không thành công"),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ")
    })
    public ResponseEntity<ApiResponseWrapper<?>> sendOtp(
            @RequestParam String email) {
        String requestId = UUID.randomUUID().toString();
        log.info("[sendOtp] RESEND_OTP_REQUEST - RequestId: {}, Email: {}", requestId, email);
        try {
            ApiResponseWrapper<?> response = customerService.reSendOtp(email);
            log.info("[sendOtp] RESEND_OTP_SUCCESS - RequestId: {}, Email: {}", requestId, email);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("[sendOtp] RESEND_OTP_FAILED - RequestId: {}, Email: {}, Error: {}", requestId, email, e.getMessage());
            return ResponseEntity.badRequest().body(
                    new ApiResponseWrapper<>(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null)
            );
        }
    }


    @PostMapping("/register/confirm")
    @Operation(summary = "Bước 3: Xác nhận đăng ký",
            description = "Xác minh mã OTP và hoàn tất đăng ký tài khoản")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Xác nhận đăng ký thành công"),
            @ApiResponse(responseCode = "400", description = "Mã OTP không hợp lệ hoặc đã hết hạn"),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ")
    })
    public ResponseEntity<ApiResponseWrapper<?>> confirmRegister(
            @RequestParam String email,
            @RequestParam String otp) {
        String requestId = UUID.randomUUID().toString();
        log.info("[confirmRegister] CONFIRM_REGISTER_REQUEST - RequestId: {}, Email: {}", requestId, email);
        try {
            ApiResponseWrapper<?> response = customerService.confirmRegister(email, otp);
            log.info("[confirmRegister] CONFIRM_REGISTER_SUCCESS - RequestId: {}, Email: {}", requestId, email);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("[confirmRegister] CONFIRM_REGISTER_FAILED - RequestId: {}, Email: {}, Error: {}", requestId, email, e.getMessage());
            return ResponseEntity.badRequest().body(
                    new ApiResponseWrapper<>(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null)
            );
        }
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Đặt lại mật khẩu",
            description = "Đặt lại mật khẩu cho tài khoản người dùng")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đặt lại mật khẩu thành công",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseWrapper.class))),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ")
    })
    public ResponseEntity<?> resetPassword(@RequestBody @Valid ResetPasswordDTO request) {
        String requestId = UUID.randomUUID().toString();
        log.info("[resetPassword] RESET_PASSWORD_REQUEST - RequestId: {}, Token: {}", requestId, request.getToken());
        try {
            ApiResponseWrapper<?> response = customerService.resetPassword(request);
            log.info("[resetPassword] RESET_PASSWORD_SUCCESS - RequestId: {}, Token: {}", requestId, request.getToken());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("[resetPassword] RESET_PASSWORD_FAILED - RequestId: {}, Token: {}, Error: {}", requestId, request.getToken(), e.getMessage());
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponseWrapper.error(e.getMessage()));
        }
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Quên mật khẩu",
            description = "Gửi liên kết đặt lại mật khẩu qua email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liên kết đặt lại mật khẩu đã được gửi",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseWrapper.class))),
            @ApiResponse(responseCode = "400", description = "Email không hợp lệ"),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ")
    })
    public ResponseEntity<?> forgotPassword(@RequestBody @Valid ForgotPasswordDTO request) {
        String requestId = UUID.randomUUID().toString();
        log.info("[forgotPassword] FORGOT_PASSWORD_REQUEST - RequestId: {}, Email: {}", requestId, request.getEmail());
        try {
            customerService.sentEmailForgotPassword(request.getEmail());
            log.info("[forgotPassword] FORGOT_PASSWORD_SUCCESS - RequestId: {}, Email: {}", requestId, request.getEmail());
            return ResponseEntity.ok(new ApiResponseWrapper<>(
                    HttpStatus.OK.value(),
                    getMessage(MessageKeys.FORGOT_PASSWORD_LINK_SENT),
                    new Response(true, getMessage(MessageKeys.FORGOT_PASSWORD_NOTIFICATION))));
        } catch (IllegalArgumentException e) {
            log.warn("[forgotPassword] FORGOT_PASSWORD_FAILED - RequestId: {}, Email: {}, Error: {}", requestId, request.getEmail(), e.getMessage());
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
    public ResponseEntity<?> getCustomerList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String keyword
    ) {
        String requestId = UUID.randomUUID().toString();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        log.info("[getCustomerList] GET_CUSTOMER_LIST_REQUEST - RequestId: {}, Page: {}, Size: {}, Keyword: {}, UserId: {}", requestId, page, size, keyword, userId);
        try {
            CustomerListResponse response = customerService.getCustomerList(page, size, keyword);
            log.info("[getCustomerList] GET_CUSTOMER_LIST_SUCCESS - RequestId: {}, UserId: {}", requestId, userId);
            return ResponseEntity.ok(new ApiResponseWrapper<>(
                    HttpStatus.OK.value(),
                    getMessage(MessageKeys.SUCCESS_GET_CUSTOMER),
                    response));
        } catch (IllegalArgumentException e) {
            log.warn("[getCustomerList] GET_CUSTOMER_LIST_FAILED - RequestId: {}, UserId: {}, Error: {}", requestId, userId, e.getMessage());
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
        String requestId = UUID.randomUUID().toString();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userID = authentication.getName();
        log.info("[getCustomerDetail] GET_CUSTOMER_DETAIL_REQUEST - RequestId: {}, UserId: {}", requestId, userID);
        try {
            CustomerResponse customer = customerService.getCustomerDetail(userID);
            log.info("[getCustomerDetail] GET_CUSTOMER_DETAIL_SUCCESS - RequestId: {}, UserId: {}", requestId, userID);
            return ResponseEntity.ok(new ApiResponseWrapper<>(
                    HttpStatus.OK.value(),
                    getMessage(MessageKeys.SUCCESS_GET_CUSTOMER),
                    customer));
        } catch (IllegalArgumentException e) {
            log.warn("[getCustomerDetail] GET_CUSTOMER_DETAIL_FAILED - RequestId: {}, UserId: {}, Error: {}", requestId, userID, e.getMessage());
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
        String requestId = UUID.randomUUID().toString();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        log.info("[getCustomerDetailByCifCode] GET_CUSTOMER_DETAIL_BY_CIF_REQUEST - RequestId: {}, CifCode: {}, UserId: {}", requestId, cifCode, userId);
        try {
            CustomerResponse customer = customerService.getCustomerDetailByCifCode(cifCode);
            log.info("[getCustomerDetailByCifCode] GET_CUSTOMER_DETAIL_BY_CIF_SUCCESS - RequestId: {}, CifCode: {}, UserId: {}", requestId, cifCode, userId);
            return ResponseEntity.ok(new ApiResponseWrapper<>(
                    HttpStatus.OK.value(),
                    getMessage(MessageKeys.SUCCESS_GET_CUSTOMER),
                    customer));
        } catch (IllegalArgumentException e) {
            log.warn("[getCustomerDetailByCifCode] GET_CUSTOMER_DETAIL_BY_CIF_FAILED - RequestId: {}, CifCode: {}, UserId: {}, Error: {}", requestId, cifCode, userId, e.getMessage());
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
        String requestId = UUID.randomUUID().toString();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userID = authentication.getName();
        log.info("[updatePassword] UPDATE_PASSWORD_REQUEST - RequestId: {}, UserId: {}", requestId, userID);
        try {
            ApiResponseWrapper<?> response = customerService.updateCustomerPassword(request);
            log.info("[updatePassword] UPDATE_PASSWORD_SUCCESS - RequestId: {}, UserId: {}", requestId, userID);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("[updatePassword] UPDATE_PASSWORD_FAILED - RequestId: {}, UserId: {}, Error: {}", requestId, userID, e.getMessage());
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
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    @PutMapping("/update")
    public ResponseEntity<?> updateCustomer(@Valid @RequestBody UpdateCustomerDTO request) {
        String requestId = UUID.randomUUID().toString();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userID = authentication.getName();
        log.info("[updateCustomer] UPDATE_CUSTOMER_REQUEST - RequestId: {}, UserId: {}", requestId, userID);
        try {
            ApiResponseWrapper<?> response = customerService.updateCustomer(request);
            log.info("[updateCustomer] UPDATE_CUSTOMER_SUCCESS - RequestId: {}, UserId: {}", requestId, userID);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("[updateCustomer] UPDATE_CUSTOMER_FAILED - RequestId: {}, UserId: {}, Error: {}", requestId, userID, e.getMessage());
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
        String requestId = UUID.randomUUID().toString();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        log.info("[updateCustomerStatus] UPDATE_STATUS_REQUEST - RequestId: {}, CifCode: {}, UserId: {}", requestId, request.getCifCode(), userId);
        try {
            ApiResponseWrapper<?> response = customerService.updateCustomerStatus(request);
            log.info("[updateCustomerStatus] UPDATE_STATUS_SUCCESS - RequestId: {}, CifCode: {}, UserId: {}", requestId, request.getCifCode(), userId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("[updateCustomerStatus] UPDATE_STATUS_FAILED - RequestId: {}, CifCode: {}, UserId: {}, Error: {}", requestId, request.getCifCode(), userId, e.getMessage());
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
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @GetMapping("/status")
    public ResponseEntity<?> checkKycStatus() {
        String requestId = UUID.randomUUID().toString();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        log.info("[checkKycStatus] CHECK_KYC_STATUS_REQUEST - RequestId: {}, UserId: {}", requestId, userId);
        try {
            KycResponse response = kycService.getKycStatus(userId);
            log.info("[checkKycStatus] CHECK_KYC_STATUS_SUCCESS - RequestId: {}, UserId: {}", requestId, userId);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            log.warn("[checkKycStatus] CHECK_KYC_STATUS_NOT_FOUND - RequestId: {}, UserId: {}, Error: {}", requestId, userId, e.getMessage());
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponseWrapper.error(e.getMessage()));
        } catch (Exception e) {
            log.error("[checkKycStatus] CHECK_KYC_STATUS_FAILED - RequestId: {}, UserId: {}, Error: {}", requestId, userId, e.getMessage(), e);
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
        String requestId = UUID.randomUUID().toString();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        log.info("[verifyKyc] VERIFY_KYC_REQUEST - RequestId: {}, UserId: {}, IdentityNumber: {}", requestId, userId, request.getIdentityNumber());
        try {
            KycResponse response = customerService.verifyKyc(userId, request);
            log.info("[verifyKyc] VERIFY_KYC_SUCCESS - RequestId: {}, UserId: {}", requestId, userId);
            return ResponseEntity.ok(new ApiResponseWrapper<>(
                    HttpStatus.OK.value(),
                    getMessage(MessageKeys.KYC_SUBMISSION_SUCCESS),
                    response
            ));
        } catch (IllegalArgumentException e) {
            log.warn("[verifyKyc] VERIFY_KYC_FAILED - RequestId: {}, UserId: {}, Error: {}", requestId, userId, e.getMessage());
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponseWrapper.error(e.getMessage()));
        }
    }

    @GetMapping("/kyc/pending")
    @Operation(summary = "Lấy danh sách KYC đang chờ duyệt",
            description = "Lấy danh sách các yêu cầu KYC có trạng thái PENDING với tìm kiếm theo từ khóa (chỉ dành cho admin)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy danh sách KYC thành công",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = KycListResponse.class))),
            @ApiResponse(responseCode = "403", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseWrapper<?>> getPendingKycRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        String requestId = UUID.randomUUID().toString();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        log.info("[getPendingKycRequests] GET_PENDING_KYC_REQUEST - RequestId: {}, Page: {}, Size: {}, Keyword: {}, UserId: {}",
                requestId, page, size, keyword, userId);
        try {
            KycListResponse response = customerService.getPendingKycRequests(page, size, keyword);
            log.info("[getPendingKycRequests] GET_PENDING_KYC_SUCCESS - RequestId: {}, UserId: {}", requestId, userId);
            return ResponseEntity.ok(new ApiResponseWrapper<>(
                    HttpStatus.OK.value(),
                    getMessage(MessageKeys.SUCCESS_GET_KYC_LIST),
                    response
            ));
        } catch (IllegalArgumentException e) {
            log.error("[getPendingKycRequests] GET_PENDING_KYC_FAILED - RequestId: {}, UserId: {}, Error: {}",
                    requestId, userId, e.getMessage(), e);
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponseWrapper.error(e.getMessage()));
        }
    }

    @PostMapping("/kyc/approve")
    @Operation(summary = "Duyệt hoặc từ chối KYC",
            description = "Admin duyệt hoặc từ chối yêu cầu KYC (VERIFIED/REJECTED)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Duyệt KYC thành công",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = KycResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "403", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseWrapper<?>> approveKyc(
            @Valid @RequestBody ApproveKycRequest request) {
        String requestId = UUID.randomUUID().toString();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        log.info("[approveKyc] APPROVE_KYC_REQUEST - RequestId: {}, CifCode: {}, KycStatus: {}, UserId: {}", requestId, request.getCifCode(), request.getStatus(), userId);
        try {
            KycResponse response = customerService.approveKyc(request.getCifCode(),
                    request.getStatus(), request.getReason());
            log.info("[approveKyc] APPROVE_KYC_SUCCESS - RequestId: {}, CifCode: {}, UserId: {}", requestId, request.getCifCode(), userId);
            return ResponseEntity.ok(new ApiResponseWrapper<>(
                    HttpStatus.OK.value(),
                    getMessage(MessageKeys.KYC_APPROVAL_SUCCESS),
                    response
            ));
        } catch (IllegalArgumentException | EntityNotFoundException e) {
            log.warn("[approveKyc] APPROVE_KYC_FAILED - RequestId: {}, CifCode: {}, UserId: {}, Error: {}", requestId, request.getCifCode(), userId, e.getMessage());
            return ResponseEntity.badRequest().body(
                    new ApiResponseWrapper<>(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null)
            );
        }
    }

    @GetMapping("/kyc/statistics")
    @Operation(summary = "Thống kê KYC",
            description = "Lấy thống kê số lượng KYC (tổng, thành công, thất bại, đang chờ) theo thời gian")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy thống kê KYC thành công",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = KycStatisticsResponse.class))),
            @ApiResponse(responseCode = "403", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseWrapper<?>> getKycStatistics(
            @RequestParam(required = false)
            LocalDate startDate,
            @RequestParam(required = false)
            LocalDate endDate) {
        String requestId = UUID.randomUUID().toString();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        log.info("[getKycStatistics] GET_KYC_STATISTICS_REQUEST - RequestId: {}, StartDate: {}, EndDate: {}, UserId: {}", requestId, startDate, endDate, userId);
        try {
            KycStatisticsResponse response = customerService.getKycStatistics(startDate, endDate);
            log.info("[getKycStatistics] GET_KYC_STATISTICS_SUCCESS - RequestId: {}, UserId: {}", requestId, userId);
            return ResponseEntity.ok(new ApiResponseWrapper<>(
                    HttpStatus.OK.value(),
                    getMessage(MessageKeys.SUCCESS_GET_KYC_STATISTICS),
                    response
            ));
        } catch (IllegalArgumentException e) {
            log.error("[getKycStatistics] GET_KYC_STATISTICS_FAILED - RequestId: {}, UserId: {}, Error: {}", requestId, userId, e.getMessage(), e);
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponseWrapper.error(e.getMessage()));
        }
    }

    @GetMapping("/growth")
    @Operation(summary = "Thống kê tăng trưởng khách hàng",
            description = "Lấy thống kê số khách hàng mới và tỷ lệ tăng trưởng theo thời gian")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy thống kê tăng trưởng thành công",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CustomerGrowthResponse.class))),
            @ApiResponse(responseCode = "403", description = "Không có quyền truy cập"),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseWrapper<?>> getCustomerGrowth(
            @RequestParam(required = false)
            LocalDate startDate,
            @RequestParam(required = false)
            LocalDate endDate) {
        String requestId = UUID.randomUUID().toString();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        log.info("[getCustomerGrowth] GET_CUSTOMER_GROWTH_REQUEST - RequestId: {}, StartDate: {}, EndDate: {}, UserId: {}", requestId, startDate, endDate, userId);
        try {
            CustomerGrowthResponse response = customerService.getCustomerGrowth(startDate, endDate);
            log.info("[getCustomerGrowth] GET_CUSTOMER_GROWTH_SUCCESS - RequestId: {}, UserId: {}", requestId, userId);
            return ResponseEntity.ok(new ApiResponseWrapper<>(
                    HttpStatus.OK.value(),
                    getMessage(MessageKeys.SUCCESS_GET_CUSTOMER_GROWTH),
                    response
            ));
        } catch (IllegalArgumentException e) {
            log.error("[getCustomerGrowth] GET_CUSTOMER_GROWTH_FAILED - RequestId: {}, UserId: {}, Error: {}", requestId, userId, e.getMessage(), e);
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponseWrapper.error(e.getMessage()));
        }
    }

    private String getMessage(String key, Object... args) {
        return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
    }

}