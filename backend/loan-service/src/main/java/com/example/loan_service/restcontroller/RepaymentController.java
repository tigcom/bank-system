package com.example.loan_service.restcontroller;

import com.example.loan_service.entity.Loan;
import com.example.loan_service.entity.Repayment;
import com.example.loan_service.handler.LoanHandler;
import com.example.loan_service.response.ApiResponseWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/repayments")
@RequiredArgsConstructor
public class RepaymentController {

    private final LoanHandler loanHandler;
    @GetMapping("/loan/{loanId}")
    public ResponseEntity<ApiResponseWrapper<List<Repayment>>> getRepaymentsByLoanId(@PathVariable Long loanId) {
        ApiResponseWrapper<List<Repayment>> response = new ApiResponseWrapper<>();
        try {
            List<Repayment> repayments = loanHandler.getRepaymentsByLoanId(loanId);
            response.setData(repayments);
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Repayments retrieved successfully");
        } catch (Exception e) {
            response.setData(null);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to retrieve repayments: " + e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

    @GetMapping("/{repaymentId}")
    public ResponseEntity<ApiResponseWrapper<Repayment>> getRepaymentById(@PathVariable Long repaymentId) {
        ApiResponseWrapper<Repayment> response = new ApiResponseWrapper<>();
        try {
            Repayment repayment = loanHandler.getRepaymentById(repaymentId)
                    .orElseThrow(() -> new RuntimeException("Repayment not found with id " + repaymentId));
            response.setData(repayment);
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Repayment found");
        } catch (Exception e) {
            response.setData(null);
            response.setStatus(HttpStatus.NOT_FOUND.value());
            response.setMessage(e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

    @PatchMapping("/{repaymentId}/late/")
    public ResponseEntity<ApiResponseWrapper<Repayment>> lateRepaymentStatus(@PathVariable Long repaymentId) {
        ApiResponseWrapper<Repayment> response = new ApiResponseWrapper<>();
        try {
            Repayment updatedRepayment = loanHandler.lateRepayment(repaymentId);
            response.setData(updatedRepayment);
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Repayment status updated successfully");
        } catch (Exception e) {
            response.setData(null);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to update repayment status: " + e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

    @PatchMapping("/{repaymentId}/unpaid/")
    public ResponseEntity<ApiResponseWrapper<Repayment>> unpaidRepaymentStatus(@PathVariable Long repaymentId) {
        ApiResponseWrapper<Repayment> response = new ApiResponseWrapper<>();
        try {
            Repayment updatedRepayment = loanHandler.unpaidRepayment(repaymentId);
            response.setData(updatedRepayment);
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Repayment status updated successfully");
        } catch (Exception e) {
            response.setData(null);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to update repayment status: " + e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

    @PostMapping("/{repaymentId}/pay")
    public ResponseEntity<ApiResponseWrapper<Repayment>> makeRepayment(@PathVariable Long repaymentId,
                                                                       @RequestParam BigDecimal amount) {
        ApiResponseWrapper<Repayment> response = new ApiResponseWrapper<>();
        try {
            Repayment repayment = loanHandler.makeRepayment(repaymentId, amount);
            response.setData(repayment);
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Repayment made successfully");
        } catch (Exception e) {
            response.setData(null);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to make repayment: " + e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

    @GetMapping("/history/{loanId}")
    public ResponseEntity<ApiResponseWrapper<List<Repayment>>> getRepaymentHistory(@PathVariable Long loanId) {
        ApiResponseWrapper<List<Repayment>> response = new ApiResponseWrapper<>();
        try {
            List<Repayment> repayment = loanHandler.getHistory(loanId);
            response.setData(repayment);
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Get history successfully");
        } catch (Exception e) {
            response.setData(null);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to get history : " + e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

    @GetMapping("/current/{loanId}")
    public ResponseEntity<ApiResponseWrapper<Repayment>> getCurrentRepayment(@PathVariable Long loanId) {
        ApiResponseWrapper<Repayment> response = new ApiResponseWrapper<>();
        try {
            Repayment repayment = loanHandler.getCurrentRepayment(loanId);
            response.setData(repayment);
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Get Current Repayment successfully");
        } catch (Exception e) {
            response.setData(null);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to get current Repayment : " + e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

    @DeleteMapping("/loan/{loanId}")
    public ResponseEntity<ApiResponseWrapper<Void>> deleteRepaymentsByLoanId(@PathVariable Long loanId) {
        ApiResponseWrapper<Void> response = new ApiResponseWrapper<>();
        try {
            loanHandler.deleteRepaymentsByLoanId(loanId);
            response.setData(null);
            response.setStatus(HttpStatus.NO_CONTENT.value());
            response.setMessage("Repayments deleted successfully");
        } catch (Exception e) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to delete repayments: " + e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }
}
