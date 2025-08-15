package com.example.loan_service.restcontroller;

import com.example.common_service.dto.CustomerResponseDTO;
import com.example.common_service.dto.response.AccountPaymentResponse;
import com.example.loan_service.dto.request.LoanRejectionReasonRequestDTO;
import com.example.common_service.dto.request.LoanRequestDTO;
import com.example.loan_service.dto.response.TransactionDto;
import com.example.loan_service.entity.Loan;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.persistence.EntityManager;

@Slf4j
@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanHandler loanHandler;
    @Autowired
    private EntityManager entityManager;
    @Operation(summary = "Tạo khoản vay mới", description = "Tạo khoản vay dựa trên thông tin đầu vào")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tạo khoản vay thành công",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseWrapper.class),
                examples = @ExampleObject(value = "{\"status\":200,\"message\":\"Successfully created Loan\",\"data\":{\"loanId\":123,\"customerId\":1,\"disbursementAccountNumber\":\"123456789\",\"repaymentAccountNumber\":\"987654321\",\"amount\":10000,\"interestRate\":5.5,\"termMonths\":12,\"status\":\"PENDING\",\"createdAt\":\"2025-08-10T10:00:00\",\"approvedAt\":null,\"loanType\":\"HOME\"}}")
            )
        ),
        @ApiResponse(responseCode = "400", description = "Dữ liệu đầu vào không hợp lệ"),
        @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi tạo khoản vay")
    })
    @PostMapping
    public ResponseEntity<ApiResponseWrapper<Loan>> createLoan(@RequestBody LoanRequestDTO loan) {
        log.info("CREATE_LOAN_START - request: {}", loan);
        ApiResponseWrapper<Loan> response = new ApiResponseWrapper<>();
        try {

            Loan created = loanHandler.createLoan(loan);
            response.setData(created);
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Successfully created Loan");
            log.info("CREATE_LOAN_SUCCESS - loanId: {}", created.getLoanId());
        } catch (IllegalArgumentException e) {
            log.warn("CREATE_LOAN_INVALID - reason: {}", e.getMessage());
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setMessage(e.getMessage());
        } catch (Exception e) {
            log.error("CREATE_LOAN_ERROR - error: {}", e.getMessage(), e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to create Loan: " + e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }
    @Operation(summary = "Cập nhật thông tin khoản vay", description = "Cập nhật khoản vay dựa trên thông tin đầu vào")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cập nhật khoản vay thành công",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseWrapper.class),
                examples = @ExampleObject(value = "{\"status\":200,\"message\":\"Successfully updated Loan\",\"data\":{\"loanId\":123,\"customerId\":1,\"disbursementAccountNumber\":\"123456789\",\"repaymentAccountNumber\":\"987654321\",\"amount\":12000,\"interestRate\":5.0,\"termMonths\":18,\"status\":\"APPROVED\",\"createdAt\":\"2025-08-10T10:00:00\",\"approvedAt\":\"2025-08-11T10:00:00\",\"loanType\":\"HOME\"}}")
            )
        ),
        @ApiResponse(responseCode = "400", description = "Dữ liệu đầu vào không hợp lệ"),
        @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi cập nhật khoản vay")
    })
    @PutMapping
    public ResponseEntity<ApiResponseWrapper<Loan>> updateLoan(@RequestBody LoanRequestDTO loan) {
        log.info("UPDATE_LOAN_START - request: {}", loan);
        ApiResponseWrapper<Loan> response = new ApiResponseWrapper<>();
        try {
            Loan updated = loanHandler.updateLoan(loan);
            response.setData(updated);
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Successfully updated Loan");
            log.info("UPDATE_LOAN_SUCCESS - loanId: {}", updated.getLoanId());
        } catch (IllegalArgumentException e) {
            log.warn("UPDATE_LOAN_INVALID - reason: {}", e.getMessage());
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setMessage(e.getMessage());
        } catch (Exception e) {
            log.error("UPDATE_LOAN_ERROR - error: {}", e.getMessage(), e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to update Loan: " + e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

    @Operation(summary = "Lấy ID khách hàng", description = "Trả về ID khách hàng hiện tại")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lấy ID khách hàng thành công",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseWrapper.class),
                examples = @ExampleObject(value = "{\"status\":200,\"message\":\"Successfully retrieved Customer ID\",\"data\":12345}")
            )
        ),
        @ApiResponse(responseCode = "400", description = "Dữ liệu đầu vào không hợp lệ"),
        @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi lấy ID khách hàng")
    })
    @GetMapping("/getCustomerId")
    public ResponseEntity<ApiResponseWrapper<Long>> getCustomerId() {
        log.info("GET_CUSTOMER_ID_START");
        ApiResponseWrapper<Long> response = new ApiResponseWrapper<>();
        try {
            Long customerId = loanHandler.getCustomerId();
            response.setData(customerId);
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Successfully retrieved Customer ID");
            log.info("GET_CUSTOMER_ID_SUCCESS - customerId: {}", customerId);
        } catch (IllegalArgumentException e) {
            log.warn("GET_CUSTOMER_ID_INVALID - reason: {}", e.getMessage());
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setMessage(e.getMessage());
        } catch (Exception e) {
            log.error("GET_CUSTOMER_ID_ERROR - error: {}", e.getMessage(), e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to get Customer ID: " + e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

    @Operation(summary = "Phê duyệt khoản vay", description = "Phê duyệt khoản vay theo loanId")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Phê duyệt khoản vay thành công",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseWrapper.class),
                examples = @ExampleObject(value = "{\"status\":200,\"message\":\"Successfully approved Loan\",\"data\":{\"loanId\":123,\"status\":\"APPROVED\"}}")
            )
        ),
        @ApiResponse(responseCode = "400", description = "Dữ liệu đầu vào không hợp lệ hoặc loanId không hợp lệ"),
        @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi phê duyệt khoản vay")
    })
//    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{loanId}/approve")
    public ResponseEntity<ApiResponseWrapper<Loan>> approveLoan(@PathVariable Long loanId) {
        log.info("APPROVE_LOAN_START - loanId: {}", loanId);
        ApiResponseWrapper<Loan> response = new ApiResponseWrapper<>();
        try {
            Loan approved = loanHandler.approveLoan(loanId);
            response.setData(approved);
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Successfully approved Loan");
            log.info("APPROVE_LOAN_SUCCESS - loanId: {}", approved.getLoanId());
        } catch (IllegalArgumentException e) {
            log.warn("APPROVE_LOAN_INVALID - loanId: {}, reason: {}", loanId, e.getMessage());
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setMessage(e.getMessage());
        } catch (Exception e) {
            log.error("APPROVE_LOAN_ERROR - loanId: {}, error: {}", loanId, e.getMessage(), e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to approve Loan: " + e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }
    @Operation(summary = "Đóng khoản vay", description = "Đóng khoản vay theo loanId. Yêu cầu quyền ADMIN")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Đóng khoản vay thành công",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseWrapper.class),
                examples = @ExampleObject(value = "``{\"status\":200,\"message\":\"Successfully closed Loan\",\"data\":{\"loanId\":123,\"status\":\"CLOSED\"}}")
            )
        ),
        @ApiResponse(responseCode = "400", description = "Dữ liệu đầu vào không hợp lệ hoặc loanId không hợp lệ"),
        @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi đóng khoản vay")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{loanId}/close")
    public ResponseEntity<ApiResponseWrapper<Loan>> closedLoan(@PathVariable Long loanId) {
        log.info("CLOSE_LOAN_START - loanId: {}", loanId);
        ApiResponseWrapper<Loan> response = new ApiResponseWrapper<>();
        try {
            Loan closed = loanHandler.closedLoan(loanId);
            response.setData(closed);
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Successfully closed Loan");
            log.info("CLOSE_LOAN_SUCCESS - loanId: {}", closed.getLoanId());
        } catch (IllegalArgumentException e) {
            log.warn("CLOSE_LOAN_INVALID - loanId: {}, reason: {}", loanId, e.getMessage());
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setMessage(e.getMessage());
        } catch (Exception e) {
            log.error("CLOSE_LOAN_ERROR - loanId: {}, error: {}", loanId, e.getMessage(), e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to close Loan: " + e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }
    @Operation(summary = "Từ chối khoản vay", description = "Từ chối khoản vay theo loanId với lý do từ chối. Yêu cầu quyền ADMIN")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Từ chối khoản vay thành công",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseWrapper.class),
                examples = @ExampleObject(value = "{\"status\":200,\"message\":\"Successfully rejected Loan\",\"data\":{\"loanId\":123,\"status\":\"REJECTED\",\"rejectionReason\":\"Lý do từ chối\"}}")
            )
        ),
        @ApiResponse(responseCode = "400", description = "Dữ liệu đầu vào không hợp lệ hoặc loanId không hợp lệ"),
        @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi từ chối khoản vay")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{loanId}/reject")
    public ResponseEntity<ApiResponseWrapper<Loan>> rejectedLoan(
            @PathVariable Long loanId,
            @RequestBody LoanRejectionReasonRequestDTO loanRejection
    ) {
        log.info("REJECT_LOAN_START - loanId: {}, reasonRequest: {}", loanId, loanRejection);
        ApiResponseWrapper<Loan> response = new ApiResponseWrapper<>();
        try {
            Loan rejected = loanHandler.rejectedLoan(loanId, loanRejection);
            response.setData(rejected);
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Successfully rejected Loan");
            log.info("REJECT_LOAN_SUCCESS - loanId: {}", rejected.getLoanId());
        } catch (IllegalArgumentException e) {
            log.warn("REJECT_LOAN_INVALID - loanId: {}, reason: {}", loanId, e.getMessage());
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setMessage(e.getMessage());
        } catch (Exception e) {
            log.error("REJECT_LOAN_ERROR - loanId: {}, error: {}", loanId, e.getMessage(), e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to reject Loan: " + e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }
    @Operation(summary = "Lấy khoản vay theo ID", description = "Truy vấn khoản vay theo loanId")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lấy khoản vay thành công",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseWrapper.class),
                examples = @ExampleObject(value = "{\"status\":200,\"message\":\"Loan found\",\"data\":{\"loanId\":123,\"amount\":10000,\"termMonths\":12,\"interestRate\":5.5,\"status\":\"APPROVED\"}}")
            )
        ),
        @ApiResponse(responseCode = "404", description = "Không tìm thấy khoản vay với ID tương ứng"),
        @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi lấy khoản vay")
    })
    @GetMapping("/{loanId}")
    public ResponseEntity<ApiResponseWrapper<Loan>> getLoanById(@PathVariable Long loanId) {
        log.info("GET_LOAN_BY_ID_START - loanId: {}", loanId);
        ApiResponseWrapper<Loan> response = new ApiResponseWrapper<>();
        try {
            Loan loan = loanHandler.getLoanById(loanId)
                    .orElseThrow(() -> new RuntimeException("Loan not found with id " + loanId));
            response.setData(loan);
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Loan found");
            log.info("GET_LOAN_BY_ID_SUCCESS - loanId: {}", loanId);
        } catch (RuntimeException e) {
            log.warn("GET_LOAN_BY_ID_NOT_FOUND - loanId: {}, reason: {}", loanId, e.getMessage());
            response.setStatus(HttpStatus.NOT_FOUND.value());
            response.setMessage(e.getMessage());
        } catch (Exception e) {
            log.error("GET_LOAN_BY_ID_ERROR - loanId: {}, error: {}", loanId, e.getMessage(), e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to get Loan: " + e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }
    @Operation(summary = "Lấy lịch sử thay đổi khoản vay theo ID", description = "Truy vấn lịch sử thay đổi của khoản vay theo loanId")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lấy lịch sử thay đổi khoản vay thành công",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseWrapper.class),
                examples = @ExampleObject(value = "{\"status\":200,\"message\":\"Lấy lịch sử thay đổi khoản vay thành công\",\"data\":[{\"loanId\":123,\"amount\":10000,\"termMonths\":12,\"interestRate\":5.5,\"status\":\"APPROVED\"},{\"loanId\":124,\"amount\":15000,\"termMonths\":18,\"interestRate\":6.0,\"status\":\"PENDING\"}]}")
            )
        ),
        @ApiResponse(responseCode = "404", description = "Không tìm thấy khoản vay với ID tương ứng"),
        @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi lấy lịch sử thay đổi khoản vay")
    })
    @GetMapping("/{loanId}/history")
    public ResponseEntity<ApiResponseWrapper<List<Loan>>> getLoanHistory(@PathVariable Long loanId) {
        ApiResponseWrapper<List<Loan>> response = new ApiResponseWrapper<>();
        try {
            AuditReader reader = AuditReaderFactory.get(entityManager);
            List<Number> revisions = reader.getRevisions(Loan.class, loanId);
            List<Loan> history = revisions.stream()
                    .map(rev -> reader.find(Loan.class, loanId, rev))
                    .toList();
            response.setData(history);
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Lấy lịch sử thay đổi khoản vay thành công");
        } catch (Exception e) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Không thể lấy lịch sử khoản vay: " + e.getMessage());
        }
        return ResponseEntity.status(response.getStatus()).body(response);
    }
    @Operation(summary = "Lấy danh sách khoản vay của khách hàng hiện tại", description = "Truy vấn tất cả khoản vay của khách hàng đang đăng nhập")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lấy danh sách khoản vay thành công",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseWrapper.class),
                examples = @ExampleObject(value = "{\"status\":200,\"message\":\"Loans retrieved successfully\",\"data\":[{\"loanId\":123,\"amount\":10000,\"termMonths\":12,\"interestRate\":5.5,\"status\":\"APPROVED\"},{\"loanId\":124,\"amount\":15000,\"termMonths\":18,\"interestRate\":6.0,\"status\":\"PENDING\"}]}")
            )
        ),
        @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi lấy danh sách khoản vay")
    })
    @GetMapping("/customer")
    public ResponseEntity<ApiResponseWrapper<List<Loan>>> getLoansByCustomerId() {
        log.info("GET_LOANS_BY_CUSTOMER_START");
        ApiResponseWrapper<List<Loan>> response = new ApiResponseWrapper<>();
        try {
            List<Loan> loans = loanHandler.getLoansByCustomerId();
            response.setData(loans);
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Loans retrieved successfully");
            log.info("GET_LOANS_BY_CUSTOMER_SUCCESS - count: {}", loans.size());
        } catch (Exception e) {
            log.error("GET_LOANS_BY_CUSTOMER_ERROR - error: {}", e.getMessage(), e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to retrieve loans: " + e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }
    @Operation(summary = "Xóa khoản vay theo ID", description = "Xóa khoản vay theo loanId. Yêu cầu quyền ADMIN")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Xóa khoản vay thành công, không có nội dung trả về"),
        @ApiResponse(responseCode = "400", description = "Dữ liệu đầu vào không hợp lệ hoặc loanId không hợp lệ"),
        @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi xóa khoản vay")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{loanId}")
    public ResponseEntity<ApiResponseWrapper<Void>> deleteLoan(@PathVariable Long loanId) {
        log.info("DELETE_LOAN_START - loanId: {}", loanId);
        ApiResponseWrapper<Void> response = new ApiResponseWrapper<>();
        try {
            loanHandler.deleteLoan(loanId);
            response.setStatus(HttpStatus.NO_CONTENT.value());
            response.setMessage("Loan deleted successfully");
            log.info("DELETE_LOAN_SUCCESS - loanId: {}", loanId);
        } catch (IllegalArgumentException e) {
            log.warn("DELETE_LOAN_INVALID - loanId: {}, reason: {}", loanId, e.getMessage());
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.setMessage(e.getMessage());
        } catch (Exception e) {
            log.error("DELETE_LOAN_ERROR - loanId: {}, error: {}", loanId, e.getMessage(), e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to delete Loan: " + e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

    @Operation(summary = "Lấy tất cả các khoản vay", description = "Truy vấn tất cả khoản vay trong hệ thống")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lấy danh sách khoản vay thành công",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseWrapper.class),
                examples = @ExampleObject(value = "{\"status\":200,\"message\":\"Loans retrieved successfully\",\"data\":[{\"loanId\":123,\"amount\":10000,\"termMonths\":12,\"interestRate\":5.5,\"status\":\"APPROVED\"},{\"loanId\":124,\"amount\":15000,\"termMonths\":18,\"interestRate\":6.0,\"status\":\"PENDING\"}]}")
            )
        ),
        @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi lấy danh sách khoản vay")
    })
    @GetMapping("/getAllloans")
    public ResponseEntity<ApiResponseWrapper<List<Loan>>> allgetLoan() {
        log.info("GET_ALL_LOANS_START");
        ApiResponseWrapper<List<Loan>> response = new ApiResponseWrapper<>();
        try {
            List<Loan> loans = loanHandler.findall();
            response.setData(loans);
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Loans retrieved successfully");
            log.info("GET_ALL_LOANS_SUCCESS - count: {}", loans.size());
        } catch (Exception e) {
            log.error("GET_ALL_LOANS_ERROR - error: {}", e.getMessage(), e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to retrieve all Loans: " + e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }
    @Operation(summary = "Lấy tổng số tiền đã vay", description = "Truy vấn tổng số tiền đã được vay từ hệ thống")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lấy tổng số tiền đã vay thành công",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseWrapper.class),
                examples = @ExampleObject(value = "{\"status\":200,\"message\":\"Total borrowed amount retrieved\",\"data\":250000.75}")
            )
        ),
        @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi lấy tổng số tiền đã vay")
    })
    @GetMapping("/total-borrowed")
    public ResponseEntity<ApiResponseWrapper<BigDecimal>> getTotalBorrowed() {
        log.info("GET_TOTAL_BORROWED_START");
        ApiResponseWrapper<BigDecimal> response = new ApiResponseWrapper<>();
        try {
            BigDecimal total = loanHandler.getTotalBorrowed();
            response.setData(total);
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Total borrowed amount retrieved");
            log.info("GET_TOTAL_BORROWED_SUCCESS - total: {}", total);
        } catch (Exception e) {
            log.error("GET_TOTAL_BORROWED_ERROR - error: {}", e.getMessage(), e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to get total borrowed: " + e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

@Operation(summary = "Lấy tổng số tiền còn nợ", description = "Truy vấn tổng số tiền còn nợ trong các khoản vay")
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Lấy tổng số tiền còn nợ thành công",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = ApiResponseWrapper.class),
            examples = @ExampleObject(value = "{\"status\":200,\"message\":\"Total outstanding amount retrieved\",\"data\":150000.50}")
        )
    ),
    @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi lấy tổng số tiền còn nợ")
})
    @GetMapping("/total-outstanding")
    public ResponseEntity<ApiResponseWrapper<BigDecimal>> getTotalOutstanding() {
        log.info("GET_TOTAL_OUTSTANDING_START");
        ApiResponseWrapper<BigDecimal> response = new ApiResponseWrapper<>();
        try {
            BigDecimal total = loanHandler.getTotalOutstanding();
            response.setData(total);
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Total outstanding amount retrieved");
            log.info("GET_TOTAL_OUTSTANDING_SUCCESS - total: {}", total);
        } catch (Exception e) {
            log.error("GET_TOTAL_OUTSTANDING_ERROR - error: {}", e.getMessage(), e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to get total outstanding: " + e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }
    @Operation(summary = "Lấy thông tin chi tiết khách hàng theo ID", description = "Truy vấn thông tin chi tiết của khách hàng dựa trên ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lấy thông tin khách hàng thành công",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseWrapper.class),
                examples = @ExampleObject(value = "{\"status\":200,\"message\":\"Customer detail retrieved successfully\",\"data\":{\"id\":123,\"name\":\"Nguyễn Văn A\",\"email\":\"nguyenvana@example.com\",\"phone\":\"0123456789\"}}")
            )
        ),
        @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi lấy thông tin khách hàng")
    })
    @GetMapping("/getCustomerById/{id}")
    public ResponseEntity<ApiResponseWrapper<CustomerResponseDTO>> getCustomerDetailById(@PathVariable Long id) {
        log.info("GET_CUSTOMER_DETAIL_START - id: {}", id);
        ApiResponseWrapper<CustomerResponseDTO> response = new ApiResponseWrapper<>();
        try {
            CustomerResponseDTO customer = loanHandler.getCustomerDetailById(id);
            response.setData(customer);
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Customer detail retrieved successfully");
            log.info("GET_CUSTOMER_DETAIL_SUCCESS - customerId: {}", id);
        } catch (Exception e) {
            log.error("GET_CUSTOMER_DETAIL_ERROR - id: {}, error: {}", id, e.getMessage(), e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to get customer detail: " + e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }
    @Operation(summary = "Lấy danh sách tài khoản thanh toán theo User ID", description = "Truy vấn danh sách tài khoản thanh toán của người dùng dựa trên User ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lấy danh sách tài khoản thành công",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseWrapper.class),
                examples = @ExampleObject(value = "{\"status\":200,\"message\":\"Account payment information retrieved successfully\",\"data\":[{\"accountId\":\"acc123\",\"bankName\":\"Ngân hàng ABC\",\"accountNumber\":\"123456789\"},{\"accountId\":\"acc124\",\"bankName\":\"Ngân hàng XYZ\",\"accountNumber\":\"987654321\"}]}")
            )
        ),
        @ApiResponse(responseCode = "500", description = "Lỗi máy chủ khi lấy thông tin tài khoản thanh toán")
    })
    @GetMapping("/getAccountsByUserId/{id}")
    public ResponseEntity<ApiResponseWrapper<List<AccountPaymentResponse>>> getAllAccountByUserId(@PathVariable String id) {
        log.info("GET_ALL_ACCOUNT_BY_USER_START - userId: {}", id);
        ApiResponseWrapper<List<AccountPaymentResponse>> response = new ApiResponseWrapper<>();
        try {
            List<AccountPaymentResponse> accounts = loanHandler.getAllPaymentAccountsbyUserId(id);
            response.setData(accounts);
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Account payment information retrieved successfully");
            log.info("GET_ALL_ACCOUNT_BY_USER_SUCCESS - userId: {}, totalAccounts: {}", id, accounts.size());
        } catch (Exception e) {
            log.error("GET_ALL_ACCOUNT_BY_USER_ERROR - userId: {}, error: {}", id, e.getMessage(), e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to get account payment information: " + e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }
    @Operation(summary = "Lấy danh sách tài khoản thanh toán của người dùng hiện tại")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lấy danh sách tài khoản thành công",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseWrapper.class),
                examples = @ExampleObject(value = "{\"status\":200,\"message\":\"Account payment information retrieved successfully\",\"data\":[{\"accountId\":\"acc123\",\"bankName\":\"Ngân hàng ABC\",\"accountNumber\":\"123456789\"}]}")
            )
        ),
        @ApiResponse(responseCode = "500", description = "Lỗi khi lấy danh sách tài khoản")
    })
    @GetMapping("/getAccounts")
    public ResponseEntity<ApiResponseWrapper<List<AccountPaymentResponse>>> getAllAccount() {
        log.info("GET_ALL_ACCOUNT");
        ApiResponseWrapper<List<AccountPaymentResponse>> response = new ApiResponseWrapper<>();
        try {
            List<AccountPaymentResponse> accounts = loanHandler.getAllPaymentAccountsbyCurrentUser();
            response.setData(accounts);
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Account payment information retrieved successfully");
            log.info("GET_ALL_ACCOUNT_BY_USER_SUCCESS  totalAccounts: {}", accounts.size());
        } catch (Exception e) {
            log.error("GET_ALL_ACCOUNT_BY_USER_ERROR  error: {}",  e.getMessage(), e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to get account payment information: " + e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }
//    @Operation(summary = "Kiểm tra thông tin thu nhập theo yêu cầu đầu vào")
//    @ApiResponses(value = {
//        @ApiResponse(responseCode = "200", description = "Lấy dữ liệu giao dịch thu nhập thành công",
//            content = @Content(mediaType = "application/json",
//                schema = @Schema(implementation = ApiResponseWrapper.class),
//                examples = @ExampleObject(value = "{\"status\":200,\"message\":\"Income transaction data retrieved successfully\",\"data\":[{\"transactionId\":\"txn001\",\"amount\":1000000,\"date\":\"2025-08-10\"}]}")
//            )
//        ),
//        @ApiResponse(responseCode = "500", description = "Lỗi khi kiểm tra thông tin thu nhập")
//    })
//    @PostMapping("/check-info-income")
//    public ResponseEntity<ApiResponseWrapper<List<TransactionDto>>> checkInfoIncome(@RequestBody InfoIncomeRequestDto infoIncome) {
//        log.info("CHECK_INFO_INCOME with info: {}", infoIncome);
//        ApiResponseWrapper<List<TransactionDto>> response = new ApiResponseWrapper<>();
//        try {
//            List<TransactionDto> transactions = loanHandler.checkInfoIncome(infoIncome);
//            response.setData(transactions);
//            response.setStatus(HttpStatus.OK.value());
//            response.setMessage("Income transaction data retrieved successfully");
//            log.info("CHECK_INFO_INCOME_SUCCESS  totalTransactions: {}", transactions.size());
//        } catch (Exception e) {
//            log.error("CHECK_INFO_INCOME_ERROR  error: {}", e.getMessage(), e);
//            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
//            response.setMessage("Failed to check income information: " + e.getMessage());
//        }
//        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
//    }
    @Operation(summary = "Lấy tổng số tiền giải ngân toàn hệ thống", description = "Chỉ dành cho ADMIN")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lấy tổng số tiền giải ngân thành công",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseWrapper.class),
                examples = @ExampleObject(value = "{\"status\":200,\"message\":\"Total disbursed amount (system-wide) retrieved\",\"data\":5000000000}")
            )
        ),
        @ApiResponse(responseCode = "500", description = "Lỗi khi lấy tổng số tiền giải ngân")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/total-disbursed")
    public ResponseEntity<ApiResponseWrapper<BigDecimal>> getTotalDisbursedSystem() {
        log.info("GET_TOTAL_DISBURSED_SYSTEM_START");
        ApiResponseWrapper<BigDecimal> response = new ApiResponseWrapper<>();
        try {
            BigDecimal total = loanHandler.getTotalDisbursedSystem();
            response.setData(total);
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Total disbursed amount (system-wide) retrieved");
            log.info("GET_TOTAL_DISBURSED_SYSTEM_SUCCESS - total: {}", total);
        } catch (Exception e) {
            log.error("GET_TOTAL_DISBURSED_SYSTEM_ERROR - error: {}", e.getMessage(), e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to get total disbursed: " + e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }
    @Operation(summary = "Lấy tổng số tiền đã thu toàn hệ thống", description = "Chỉ dành cho ADMIN")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lấy tổng số tiền thu thành công",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseWrapper.class),
                examples = @ExampleObject(value = "{\"status\":200,\"message\":\"Total collected amount (system-wide) retrieved\",\"data\":4000000000}")
            )
        ),
        @ApiResponse(responseCode = "500", description = "Lỗi khi lấy tổng số tiền thu")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/total-collected")
    public ResponseEntity<ApiResponseWrapper<BigDecimal>> getTotalCollectedSystem() {
        log.info("GET_TOTAL_COLLECTED_SYSTEM_START");
        ApiResponseWrapper<BigDecimal> response = new ApiResponseWrapper<>();
        try {
            BigDecimal total = loanHandler.getTotalCollectedSystem();
            response.setData(total);
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Total collected amount (system-wide) retrieved");
            log.info("GET_TOTAL_COLLECTED_SYSTEM_SUCCESS - total: {}", total);
        } catch (Exception e) {
            log.error("GET_TOTAL_COLLECTED_SYSTEM_ERROR - error: {}", e.getMessage(), e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to get total collected: " + e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }
    @Operation(summary = "Lấy tổng lợi nhuận toàn hệ thống", description = "Chỉ dành cho ADMIN")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lấy tổng lợi nhuận thành công",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseWrapper.class),
                examples = @ExampleObject(value = "{\"status\":200,\"message\":\"Total profit (system-wide) retrieved\",\"data\":1000000000}")
            )
        ),
        @ApiResponse(responseCode = "500", description = "Lỗi khi lấy tổng lợi nhuận")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/total-profit")
    public ResponseEntity<ApiResponseWrapper<BigDecimal>> getTotalProfitSystem() {
        log.info("GET_TOTAL_PROFIT_SYSTEM_START");
        ApiResponseWrapper<BigDecimal> response = new ApiResponseWrapper<>();
        try {
            BigDecimal total = loanHandler.getTotalProfitSystem();
            response.setData(total);
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Total profit (system-wide) retrieved");
            log.info("GET_TOTAL_PROFIT_SYSTEM_SUCCESS - total: {}", total);
        } catch (Exception e) {
            log.error("GET_TOTAL_PROFIT_SYSTEM_ERROR - error: {}", e.getMessage(), e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to get total profit: " + e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

}
