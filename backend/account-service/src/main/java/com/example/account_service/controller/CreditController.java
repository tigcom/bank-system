package com.example.account_service.controller;

import com.example.account_service.dto.request.CreditRequestCreateDTO;
import com.example.account_service.dto.request.CreditRequestConfirmDTO;
import com.example.account_service.dto.request.PaymentCreateDTO;
import com.example.account_service.dto.request.SavingCreateDTO;
import com.example.account_service.dto.response.AccountCreateReponse;
import com.example.account_service.dto.response.ApiResponseWrapper;
import com.example.account_service.dto.response.CreditRequestReponse;
import com.example.account_service.service.AccountService;
import com.example.account_service.service.CreditRequestService;
import com.example.account_service.utils.MessageUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Credit Request", description = "APIs for creating and managing credit requests")
@RestController
@RequiredArgsConstructor
public class CreditController {

    private final AccountService accountService;
    private final CreditRequestService creditRequestService;
    private final MessageUtils messageUtils;

    @Operation(
            summary = "Initiate Credit Request (Step 1)",
            description = "Khởi tạo yêu cầu thẻ tín dụng và gửi OTP đến email. Khách hàng cần xác thực OTP để hoàn tất yêu cầu."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "OTP sent successfully, please verify to complete request"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/initiate-credit-request")
    public ApiResponseWrapper<CreditRequestReponse> initiateCreditRequest(@Valid @RequestBody CreditRequestCreateDTO creditRequestCreateDTO) {
        CreditRequestReponse response = creditRequestService.createCreditRequest(creditRequestCreateDTO);
        return ApiResponseWrapper.<CreditRequestReponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("OTP đã được gửi đến email của bạn. Vui lòng xác thực để hoàn tất yêu cầu.")
                .data(response)
                .build();
    }

    @Operation(
            summary = "Resend OTP for Credit Request",
            description = "Gửi lại OTP cho yêu cầu thẻ tín dụng. OTP có hiệu lực trong 10 phút."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OTP sent successfully"),
            @ApiResponse(responseCode = "404", description = "Credit request not found or expired"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/resend-otp-credit/{tempRequestKey}")
    public ApiResponseWrapper<String> resendOTPForCreditRequest(@PathVariable String tempRequestKey) {
        creditRequestService.sendOTP(tempRequestKey);
        return ApiResponseWrapper.<String>builder()
                .status(HttpStatus.OK.value())
                .message("OTP đã được gửi lại đến email của bạn.")
                .data("OTP resent successfully.")
                .build();
    }

    @Operation(
            summary = "Confirm OTP and Create Credit Request (Step 2)",
            description = "Xác thực OTP và tạo yêu cầu thẻ tín dụng chờ duyệt. Sau khi xác thực thành công, yêu cầu sẽ được chuyển đến admin để xem xét."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "OTP verified and credit request created, waiting for admin review"),
            @ApiResponse(responseCode = "400", description = "Invalid OTP or request expired"),
            @ApiResponse(responseCode = "404", description = "Request not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/confirm-otp-credit")
    public ApiResponseWrapper<CreditRequestReponse> confirmOTPAndCreateRequest(@Valid @RequestBody CreditRequestConfirmDTO creditRequestConfirmDTO) {
        CreditRequestReponse response = creditRequestService.confirmOTPAndCreateAccount(creditRequestConfirmDTO);
        return ApiResponseWrapper.<CreditRequestReponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Xác thực OTP thành công! Yêu cầu thẻ tín dụng đã được tạo và đang chờ duyệt.")
                .data(response)
                .build();
    }

    @Operation(
            summary = "Approve Credit Request (Admin Only)",
            description = "Phê duyệt yêu cầu thẻ tín dụng. Kiểm tra các điều kiện về tuổi tác, thu nhập và tạo tài khoản tín dụng nếu đạt yêu cầu. Email thông báo sẽ được gửi tự động."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Credit request approved and account created successfully"),
            @ApiResponse(responseCode = "404", description = "Credit request not found"),
            @ApiResponse(responseCode = "400", description = "Invalid credit request status or business rules failed"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/admin/approve-credit-request/{id}")
    public ApiResponseWrapper<AccountCreateReponse> approveCreditRequest(@PathVariable String id) {
        AccountCreateReponse response = creditRequestService.approveCreditRequest(id);
        return ApiResponseWrapper.<AccountCreateReponse>builder()
                .status(HttpStatus.OK.value())
                .message(messageUtils.getMessage("account.credit.approved"))
                .data(response)
                .build();
    }

    @Operation(
            summary = "Reject Credit Request (Admin Only)",
            description = "Từ chối yêu cầu thẻ tín dụng và gửi email thông báo đến khách hàng với lý do từ chối và gợi ý cải thiện."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Credit request rejected successfully"),
            @ApiResponse(responseCode = "404", description = "Credit request not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/admin/reject-credit-request/{id}")
    public ApiResponseWrapper<CreditRequestReponse> rejectCreditRequest(@PathVariable String id) {
        CreditRequestReponse response = creditRequestService.rejectCreditRequest(id);
        return ApiResponseWrapper.<CreditRequestReponse>builder()
                .status(HttpStatus.OK.value())
                .message(messageUtils.getMessage("account.credit.reject"))
                .data(response)
                .build();
    }

    @Operation(
            summary = "Get All Credit Requests (Admin Only)",
            description = "Lấy danh sách tất cả yêu cầu thẻ tín dụng trong hệ thống để admin review."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of credit requests"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/admin/get-all-credit-request")
    public ApiResponseWrapper<List<CreditRequestReponse>> getAllCreditRequest() {
        List<CreditRequestReponse> list = creditRequestService.getAllCreditRequest();
        return ApiResponseWrapper.<List<CreditRequestReponse>>builder()
                .status(HttpStatus.OK.value())
                .message(messageUtils.getMessage("account.credit-request.list"))
                .data(list)
                .build();
    }
}
