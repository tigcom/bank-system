package com.example.loan_service.restcontroller;

import com.example.loan_service.entity.Repayment;
import com.example.loan_service.handler.LoanHandler;
import com.example.loan_service.response.ApiResponseWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/repayments")
@RequiredArgsConstructor
public class RepaymentController {

    private final LoanHandler loanHandler;

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
}
