package com.example.loan_service.restcontroller;

import com.example.loan_service.entity.Repayment;
import com.example.loan_service.handler.LoanHandler;
import com.example.loan_service.dto.response.ApiResponseWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ExampleObject;

@Slf4j
@RestController
@RequestMapping("/api/repayments")
@RequiredArgsConstructor
public class RepaymentController {

    private final LoanHandler loanHandler;

    // 1. Lấy danh sách kỳ trả nợ theo khoản vay
    @Operation(summary = "Lấy danh sách kỳ trả nợ theo khoản vay", description = "Trả về danh sách các kỳ trả nợ của một khoản vay cụ thể.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Thành công",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseWrapper.class),
                examples = @ExampleObject(value = "{\"status\":200,\"message\":\"Repayments retrieved successfully\",\"data\":[{\"repaymentId\":1,\"dueDate\":\"2025-08-10\",\"principal\":1000,\"interest\":50,\"paidAmount\":0,\"status\":\"UNPAID\"}]}")
            )
        ),
        @ApiResponse(responseCode = "404", description = "Không tìm thấy khoản vay")
    })
    @GetMapping("/loan/{loanId}")
    public ResponseEntity<ApiResponseWrapper<List<Repayment>>> getRepaymentsByLoanId(@PathVariable Long loanId) {
        log.info("GET_REPAYMENTS_BY_LOAN_ID_START - loanId: {}", loanId);
        ApiResponseWrapper<List<Repayment>> response = new ApiResponseWrapper<>();
        try {
            List<Repayment> repayments = loanHandler.getRepaymentsByLoanId(loanId);
            response.setData(repayments);
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Repayments retrieved successfully");
            log.info("GET_REPAYMENTS_BY_LOAN_ID_SUCCESS - loanId: {}, count: {}", loanId, repayments.size());
        } catch (Exception e) {
            log.error("GET_REPAYMENTS_BY_LOAN_ID_ERROR - loanId: {}, error: {}", loanId, e.getMessage(), e);
            response.setData(null);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to retrieve repayments: " + e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

    // 2. Lấy chi tiết một kỳ trả nợ
    @Operation(summary = "Lấy chi tiết một kỳ trả nợ", description = "Trả về thông tin chi tiết của một kỳ trả nợ theo mã.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Thành công",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseWrapper.class),
                examples = @ExampleObject(value = "{\"status\":200,\"message\":\"Repayment found\",\"data\":{\"repaymentId\":1,\"dueDate\":\"2025-08-10\",\"principal\":1000,\"interest\":50,\"paidAmount\":0,\"status\":\"UNPAID\"}}")
            )
        ),
        @ApiResponse(responseCode = "404", description = "Không tìm thấy kỳ trả nợ")
    })
    @GetMapping("/{repaymentId}")
    public ResponseEntity<ApiResponseWrapper<Repayment>> getRepaymentById(@PathVariable Long repaymentId) {
        log.info("GET_REPAYMENT_BY_ID_START - repaymentId: {}", repaymentId);
        ApiResponseWrapper<Repayment> response = new ApiResponseWrapper<>();
        try {
            Repayment repayment = loanHandler.getRepaymentById(repaymentId)
                    .orElseThrow(() -> new RuntimeException("Repayment not found with id " + repaymentId));
            response.setData(repayment);
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Repayment found");
            log.info("GET_REPAYMENT_BY_ID_SUCCESS - repaymentId: {}", repaymentId);
        } catch (RuntimeException e) {
            log.warn("GET_REPAYMENT_BY_ID_NOT_FOUND - repaymentId: {}, reason: {}", repaymentId, e.getMessage());
            response.setData(null);
            response.setStatus(HttpStatus.NOT_FOUND.value());
            response.setMessage(e.getMessage());
        } catch (Exception e) {
            log.error("GET_REPAYMENT_BY_ID_ERROR - repaymentId: {}, error: {}", repaymentId, e.getMessage(), e);
            response.setData(null);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to retrieve repayment: " + e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

    // 3. Đánh dấu kỳ trả nợ là LATE
    @Operation(summary = "Đánh dấu kỳ trả nợ là LATE", description = "Chỉ ADMIN mới thực hiện được.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Đánh dấu thành công",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseWrapper.class),
                examples = @ExampleObject(value = "{\"status\":200,\"message\":\"Repayment marked as late successfully\",\"data\":{\"repaymentId\":1,\"status\":\"LATE\"}}")
            )
        ),
        @ApiResponse(responseCode = "400", description = "Dữ liệu đầu vào không hợp lệ hoặc repaymentId không hợp lệ")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{repaymentId}/late")
    public ResponseEntity<ApiResponseWrapper<Repayment>> lateRepaymentStatus(@PathVariable Long repaymentId) {
        log.info("LATE_REPAYMENT_START - repaymentId: {}", repaymentId);
        ApiResponseWrapper<Repayment> response = new ApiResponseWrapper<>();
        try {
            Repayment updated = loanHandler.lateRepayment(repaymentId);
            response.setData(updated);
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Repayment marked as late successfully");
            log.info("LATE_REPAYMENT_SUCCESS - repaymentId: {}", repaymentId);
        } catch (IllegalArgumentException e) {
            log.warn("LATE_REPAYMENT_INVALID - repaymentId: {}, reason: {}", repaymentId, e.getMessage());
            response.setData(null);
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setMessage(e.getMessage());
        } catch (Exception e) {
            log.error("LATE_REPAYMENT_ERROR - repaymentId: {}, error: {}", repaymentId, e.getMessage(), e);
            response.setData(null);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to mark repayment as late: " + e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

    // 4. Đánh dấu kỳ trả nợ là UNPAID
    @Operation(summary = "Đánh dấu kỳ trả nợ là UNPAID", description = "Chỉ ADMIN mới thực hiện được.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Đánh dấu thành công",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseWrapper.class),
                examples = @ExampleObject(value = "{\"status\":200,\"message\":\"Repayment marked as unpaid successfully\",\"data\":{\"repaymentId\":1,\"status\":\"UNPAID\"}}")
            )
        ),
        @ApiResponse(responseCode = "400", description = "Dữ liệu đầu vào không hợp lệ hoặc repaymentId không hợp lệ")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{repaymentId}/unpaid")
    public ResponseEntity<ApiResponseWrapper<Repayment>> unpaidRepaymentStatus(@PathVariable Long repaymentId) {
        log.info("UNPAID_REPAYMENT_START - repaymentId: {}", repaymentId);
        ApiResponseWrapper<Repayment> response = new ApiResponseWrapper<>();
        try {
            Repayment updated = loanHandler.unpaidRepayment(repaymentId);
            response.setData(updated);
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Repayment marked as unpaid successfully");
            log.info("UNPAID_REPAYMENT_SUCCESS - repaymentId: {}", repaymentId);
        } catch (IllegalArgumentException e) {
            log.warn("UNPAID_REPAYMENT_INVALID - repaymentId: {}, reason: {}", repaymentId, e.getMessage());
            response.setData(null);
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setMessage(e.getMessage());
        } catch (Exception e) {
            log.error("UNPAID_REPAYMENT_ERROR - repaymentId: {}, error: {}", repaymentId, e.getMessage(), e);
            response.setData(null);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to mark repayment as unpaid: " + e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

    // 5. Tạo yêu cầu thanh toán kỳ trả nợ (gửi OTP)
    @Operation(summary = "Tạo yêu cầu thanh toán kỳ trả nợ (gửi OTP)", description = "Gửi OTP xác thực thanh toán cho kỳ trả nợ.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OTP gửi thành công",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseWrapper.class),
                examples = @ExampleObject(value = "{\"status\":200,\"message\":\"OTP sent successfully\",\"data\":\"refCode123\"}")
            )
        ),
        @ApiResponse(responseCode = "400", description = "Dữ liệu đầu vào không hợp lệ hoặc repaymentId không hợp lệ")
    })
    @PostMapping("/{repaymentId}/pay")
    public ResponseEntity<ApiResponseWrapper<String>> makeRepayment(
            @PathVariable Long repaymentId,
            @RequestParam BigDecimal amount,
            @RequestParam String accountNumber
    ) {
        log.info("MAKE_REPAYMENT_START - repaymentId: {}, amount: {}, accountNumber: {}", repaymentId, amount, accountNumber);
        ApiResponseWrapper<String> response = new ApiResponseWrapper<>();
        try {
            String refCode = loanHandler.makeRepayment(repaymentId, amount, accountNumber);
            response.setData(refCode);
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("OTP sent successfully");
            log.info("MAKE_REPAYMENT_SUCCESS - repaymentId: {}, referenceCode: {}", repaymentId, refCode);
        } catch (IllegalArgumentException e) {
            log.warn("MAKE_REPAYMENT_INVALID - repaymentId: {}, reason: {}", repaymentId, e.getMessage());
            response.setData(null);
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setMessage(e.getMessage());
        } catch (Exception e) {
            log.error("MAKE_REPAYMENT_ERROR - repaymentId: {}, error: {}", repaymentId, e.getMessage(), e);
            response.setData(null);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to make repayment: " + e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

    // 6. Xác nhận thanh toán kỳ trả nợ (nhập OTP)
    @Operation(summary = "Xác nhận thanh toán kỳ trả nợ (nhập OTP)", description = "Xác nhận thanh toán cho kỳ trả nợ bằng OTP.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Xác nhận thanh toán thành công",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseWrapper.class),
                examples = @ExampleObject(value = "{\"status\":200,\"message\":\"Repayment confirmed successfully\",\"data\":{\"repaymentId\":1,\"status\":\"PAID\"}}")
            )
        ),
        @ApiResponse(responseCode = "400", description = "Dữ liệu đầu vào không hợp lệ hoặc repaymentId không hợp lệ")
    })
    @PostMapping("/{repaymentId}/confirm")
    public ResponseEntity<ApiResponseWrapper<Repayment>> confirmRepayment(
            @PathVariable Long repaymentId,
            @RequestParam BigDecimal amount,
            @RequestParam String otpCode,
            @RequestParam String referenceCode
    ) {
        log.info("CONFIRM_REPAYMENT_START - repaymentId: {}, amount: {}, referenceCode: {}", repaymentId, amount, referenceCode);
        ApiResponseWrapper<Repayment> response = new ApiResponseWrapper<>();
        try {
            Repayment confirmed = loanHandler.confirmRepayment(repaymentId, amount, otpCode, referenceCode);
            response.setData(confirmed);
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Repayment confirmed successfully");
            log.info("CONFIRM_REPAYMENT_SUCCESS - repaymentId: {}", repaymentId);
        } catch (IllegalArgumentException e) {
            log.warn("CONFIRM_REPAYMENT_INVALID - repaymentId: {}, reason: {}", repaymentId, e.getMessage());
            response.setData(null);
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setMessage(e.getMessage());
        } catch (Exception e) {
            log.error("CONFIRM_REPAYMENT_ERROR - repaymentId: {}, error: {}", repaymentId, e.getMessage(), e);
            response.setData(null);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to confirm repayment: " + e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

    // 7. Lịch sử trả nợ
    @Operation(summary = "Lấy lịch sử trả nợ của khách hàng hiện tại", description = "Trả về danh sách các kỳ trả nợ đã thực hiện.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lấy lịch sử trả nợ thành công",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseWrapper.class),
                examples = @ExampleObject(value = "{\"status\":200,\"message\":\"Repayment history retrieved successfully\",\"data\":[{\"repaymentId\":1,\"dueDate\":\"2025-08-10\",\"principal\":1000,\"interest\":50,\"paidAmount\":1000,\"status\":\"PAID\"}]}")
            )
        ),
        @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi lấy lịch sử trả nợ")
    })
    @GetMapping("/history")
    public ResponseEntity<ApiResponseWrapper<List<Repayment>>> getRepaymentHistory() {
        log.info("GET_REPAYMENT_HISTORY_START");
        ApiResponseWrapper<List<Repayment>> response = new ApiResponseWrapper<>();
        try {
            List<Repayment> history = loanHandler.getHistory();
            response.setData(history);
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Repayment history retrieved successfully");
            log.info("GET_REPAYMENT_HISTORY_SUCCESS - count: {}", history.size());
        } catch (Exception e) {
            log.error("GET_REPAYMENT_HISTORY_ERROR - error: {}", e.getMessage(), e);
            response.setData(null);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to get repayment history: " + e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

    // 8. Kỳ trả nợ hiện tại
    @Operation(summary = "Lấy kỳ trả nợ hiện tại", description = "Trả về kỳ trả nợ tiếp theo của khách hàng hiện tại.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lấy kỳ trả nợ hiện tại thành công",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseWrapper.class),
                examples = @ExampleObject(value = "{\"status\":200,\"message\":\"Current repayment retrieved successfully\",\"data\":{\"repaymentId\":2,\"dueDate\":\"2025-09-10\",\"principal\":1000,\"interest\":45,\"paidAmount\":0,\"status\":\"UNPAID\"}}")
            )
        ),
        @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi lấy kỳ trả nợ hiện tại")
    })
    @GetMapping("/current")
    public ResponseEntity<ApiResponseWrapper<Repayment>> getCurrentRepayment() {
        log.info("GET_CURRENT_REPAYMENT_START");
        ApiResponseWrapper<Repayment> response = new ApiResponseWrapper<>();
        try {
            Repayment current = loanHandler.getCurrentRepayment();
            response.setData(current);
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Current repayment retrieved successfully");
            log.info("GET_CURRENT_REPAYMENT_SUCCESS - repaymentId: {}", current.getRepaymentId());
        } catch (Exception e) {
            log.error("GET_CURRENT_REPAYMENT_ERROR - error: {}", e.getMessage(), e);
            response.setData(null);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to get current repayment: " + e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

    // 9. Xóa tất cả kỳ trả nợ của khoản vay
    @Operation(summary = "Xóa tất cả kỳ trả nợ của khoản vay", description = "Xóa toàn bộ các kỳ trả nợ của một khoản vay.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Xóa thành công, không có nội dung trả về"),
        @ApiResponse(responseCode = "400", description = "Dữ liệu đầu vào không hợp lệ hoặc loanId không hợp lệ"),
        @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi xóa các kỳ trả nợ")
    })
    @DeleteMapping("/loan/{loanId}")
    public ResponseEntity<ApiResponseWrapper<Void>> deleteRepaymentsByLoanId(@PathVariable Long loanId) {
        log.info("DELETE_REPAYMENTS_BY_LOAN_ID_START - loanId: {}", loanId);
        ApiResponseWrapper<Void> response = new ApiResponseWrapper<>();
        try {
            loanHandler.deleteRepaymentsByLoanId(loanId);
            response.setStatus(HttpStatus.NO_CONTENT.value());
            response.setMessage("Repayments deleted successfully");
            log.info("DELETE_REPAYMENTS_BY_LOAN_ID_SUCCESS - loanId: {}", loanId);
        } catch (IllegalArgumentException e) {
            log.warn("DELETE_REPAYMENTS_BY_LOAN_ID_INVALID - loanId: {}, reason: {}", loanId, e.getMessage());
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setMessage(e.getMessage());
        } catch (Exception e) {
            log.error("DELETE_REPAYMENTS_BY_LOAN_ID_ERROR - loanId: {}, error: {}", loanId, e.getMessage(), e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to delete repayments: " + e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

    // 10. Thống kê trạng thái trả nợ
    @Operation(summary = "Thống kê trạng thái trả nợ", description = "Trả về thống kê số lượng các kỳ trả nợ theo trạng thái. Chỉ ADMIN mới thực hiện được.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lấy thống kê thành công",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseWrapper.class),
                examples = @ExampleObject(value = "{\"status\":200,\"message\":\"Repayment stats retrieved successfully\",\"data\":{\"paid\":5,\"unpaid\":2,\"late\":1}}")
            )
        ),
        @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi lấy thống kê")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponseWrapper<Map<String, Long>>> getRepaymentStats() {
        log.info("GET_REPAYMENT_STATS_START");
        ApiResponseWrapper<Map<String, Long>> response = new ApiResponseWrapper<>();
        try {
            Map<String, Long> stats = loanHandler.getRepaymentStats();
            response.setData(stats);
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Repayment stats retrieved successfully");
            log.info("GET_REPAYMENT_STATS_SUCCESS - {}", stats);
        } catch (Exception e) {
            log.error("GET_REPAYMENT_STATS_ERROR - {}", e.getMessage(), e);
            response.setData(null);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to get repayment stats: " + e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

}
