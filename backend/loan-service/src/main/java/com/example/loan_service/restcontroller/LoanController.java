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
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanHandler loanHandler;

    @PostMapping
    public ResponseEntity<ApiResponseWrapper<Loan>> createLoan(@RequestBody Loan loan) {
        ApiResponseWrapper<Loan> response = new ApiResponseWrapper<>();
        try {
            response.setData(loanHandler.createLoan(loan));
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Successfully created Loan");
        } catch (Exception e) {
            response.setData(null);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to create Loan: " + e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

    @PutMapping
    public ResponseEntity<ApiResponseWrapper<Loan>> updateLoan(@RequestBody Loan loan) {
        ApiResponseWrapper<Loan> response = new ApiResponseWrapper<>();
        try {
            response.setData(loanHandler.updateLoan(loan));
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Successfully update Loan");
        } catch (Exception e) {
            response.setData(null);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to update Loan: " + e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

    @PostMapping("/{loanId}/approve")
    public ResponseEntity<ApiResponseWrapper<Loan>> approveLoan(@PathVariable Long loanId) {
        ApiResponseWrapper<Loan> response = new ApiResponseWrapper<>();
        try {
            response.setData(loanHandler.approveLoan(loanId));
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Successfully approved Loan");
        } catch (Exception e) {
            response.setData(null);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to approve Loan: " + e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

    @GetMapping("/{loanId}")
    public ResponseEntity<ApiResponseWrapper<Loan>> getLoanById(@PathVariable Long loanId) {
        ApiResponseWrapper<Loan> response = new ApiResponseWrapper<>();
        try {
            Loan loan = loanHandler.getLoanById(loanId)
                    .orElseThrow(() -> new RuntimeException("Loan not found with id " + loanId));
            response.setData(loan);
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Loan found");
        } catch (Exception e) {
            response.setData(null);
            response.setStatus(HttpStatus.NOT_FOUND.value());
            response.setMessage(e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponseWrapper<List<Loan>>> getLoansByCustomerId(@PathVariable Long customerId) {
        ApiResponseWrapper<List<Loan>> response = new ApiResponseWrapper<>();
        try {
            List<Loan> loans = loanHandler.getLoansByCustomerId(customerId);
            response.setData(loans);
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Loans retrieved successfully");
        } catch (Exception e) {
            response.setData(null);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to retrieve loans: " + e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

    @PatchMapping("/{loanId}/status")
    public ResponseEntity<ApiResponseWrapper<Loan>> updateLoanStatus(@PathVariable Long loanId,
                                                                     @RequestParam String status) {
        ApiResponseWrapper<Loan> response = new ApiResponseWrapper<>();
        try {
            Loan updatedLoan = loanHandler.updateLoanStatus(loanId, status);
            response.setData(updatedLoan);
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Loan status updated successfully");
        } catch (Exception e) {
            response.setData(null);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to update loan status: " + e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

    @DeleteMapping("/{loanId}")
    public ResponseEntity<ApiResponseWrapper<Void>> deleteLoan(@PathVariable Long loanId) {
        ApiResponseWrapper<Void> response = new ApiResponseWrapper<>();
        try {
            loanHandler.deleteLoan(loanId);
            response.setData(null);
            response.setStatus(HttpStatus.NO_CONTENT.value());
            response.setMessage("Loan deleted successfully");
        } catch (Exception e) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to delete loan: " + e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

    @GetMapping("/{loanId}/repayments")
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

    @PostMapping("/repayments/{repaymentId}/pay")
    public ResponseEntity<ApiResponseWrapper<Repayment>> makeRepayment(@PathVariable Long repaymentId,
                                                                       @RequestParam BigDecimal amount) {
        ApiResponseWrapper<Repayment> response = new ApiResponseWrapper<>();
        try {
            Repayment repayment = loanHandler.makeRepayment(repaymentId, amount);
            response.setData(repayment);
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Repayment successful");
        } catch (Exception e) {
            response.setData(null);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to make repayment: " + e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

    @PatchMapping("/repayments/{repaymentId}/status")
    public ResponseEntity<ApiResponseWrapper<Repayment>> updateRepaymentStatus(@PathVariable Long repaymentId,
                                                                               @RequestParam String status) {
        ApiResponseWrapper<Repayment> response = new ApiResponseWrapper<>();
        try {
            Repayment repayment = loanHandler.updateRepaymentStatus(repaymentId, status);
            response.setData(repayment);
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Repayment status updated successfully");
        } catch (Exception e) {
            response.setData(null);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to update repayment status: " + e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

    @DeleteMapping("/{loanId}/repayments")
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
