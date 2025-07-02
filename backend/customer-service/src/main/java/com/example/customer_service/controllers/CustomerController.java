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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        try {
            log.info("Khởi tạo đăng ký cho email: {}", request.getEmail());
            ApiResponseWrapper<?> response = customerService.initiateRegister(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Dữ liệu đăng ký không hợp lệ: {}", e.getMessage());
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
        try {
            log.info("Xác minh KYC và gửi OTP cho email: {}", email);
            ApiResponseWrapper<?> response = customerService.processKycAndSendOtp(email, kycRequest);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Thông tin KYC không hợp lệ với email {}: {}", email, e.getMessage());
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
        try {
            log.info("Gửi OTP cho email: {}", email);
            ApiResponseWrapper<?> response = customerService.reSendOtp(email);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Gửi otp không thành công cho email {}: {}", email, e.getMessage());
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
        try {
            log.info("Xác nhận đăng ký cho email: {}", email);
            ApiResponseWrapper<?> response = customerService.confirmRegister(email, otp);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Mã OTP không hợp lệ với email {}: {}", email, e.getMessage());
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
    @Operation(summary = "Quên mật khẩu",
            description = "Gửi liên kết đặt lại mật khẩu qua email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liên kết đặt lại mật khẩu đã được gửi",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseWrapper.class))),
            @ApiResponse(responseCode = "400", description = "Email không hợp lệ"),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ")
    })
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
    public ResponseEntity<?> getCustomerList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String keyword
    ) {
        try {
            log.info("Fetching customer list - page: {}, size: {}, search: {}", page, size, keyword);
            CustomerListResponse response = customerService.getCustomerList(page, size, keyword);
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
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
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
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @GetMapping("/status")
    public ResponseEntity<?> checkKycStatus() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userId = authentication.getName();

            log.info("Kiểm tra trạng thái KYC cho userId: {}", userId);
            KycResponse response = kycService.getKycStatus(userId);

            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponseWrapper.error(e.getMessage()));
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
    @Operation(summary = "Lấy danh sách tài khoản", description = "Truy vấn danh sách tài khoản của khách hàng")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy danh sách tài khoản thành công",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AccountDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "500", description = "Lỗi máy chủ")
    })
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> getCustomerAccounts() {
        try {
            log.info("Start get list account");
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userID = authentication.getName();
            Customer customer = customerRepository.findCustomerByUserId(userID);
            log.info("Get list account for username: {}", customer.getUsername());

            SecurityContextHolder.clearContext();

            List<AccountDTO> accounts = customerCommonService.getAccountsByCifCode(customer.getCifCode());
            log.info("List account: {}", accounts);
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