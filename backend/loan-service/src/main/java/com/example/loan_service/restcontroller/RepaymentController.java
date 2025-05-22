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
    @PostMapping("/generate")
    public ResponseEntity<ApiResponseWrapper<List<Repayment>>> generateRepaymentSchedule(@RequestBody Loan loan) {
        ApiResponseWrapper<List<Repayment>> response = new ApiResponseWrapper<>();
        try {
            List<Repayment> schedule = loanHandler.generateRepaymentSchedule(loan);
            response.setData(schedule);
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Repayment schedule generated successfully");
        } catch (Exception e) {
            response.setData(null);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to generate repayment schedule: " + e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

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

    @PatchMapping("/{repaymentId}/status")
    public ResponseEntity<ApiResponseWrapper<Repayment>> updateRepaymentStatus(@PathVariable Long repaymentId,
                                                                               @RequestParam String status) {
        ApiResponseWrapper<Repayment> response = new ApiResponseWrapper<>();
        try {
            Repayment updatedRepayment = loanHandler.updateRepaymentStatus(repaymentId, status);
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

    @PostMapping("/update-schedule")
    public ResponseEntity<ApiResponseWrapper<List<Repayment>>> updateRepaymentSchedule(
            @RequestBody Loan loan,
            @RequestParam int startPeriodIndex,
            @RequestParam BigDecimal remainingPrincipal) {
        ApiResponseWrapper<List<Repayment>> response = new ApiResponseWrapper<>();
        try {
            List<Repayment> updatedSchedule = loanHandler.updateRepaymentSchedule(loan, startPeriodIndex, remainingPrincipal);
            response.setData(updatedSchedule);
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Repayment schedule updated successfully");
        } catch (Exception e) {
            response.setData(null);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to update repayment schedule: " + e.getMessage());
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
