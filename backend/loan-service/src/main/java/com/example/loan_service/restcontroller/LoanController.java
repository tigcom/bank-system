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

    @PostMapping("/{loanId}/close")
    public ResponseEntity<ApiResponseWrapper<Loan>> closedLoan(@PathVariable Long loanId) {
        ApiResponseWrapper<Loan> response = new ApiResponseWrapper<>();
        try {
            response.setData(loanHandler.closedLoan(loanId));
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Successfully approved Loan");
        } catch (Exception e) {
            response.setData(null);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to close Loan: " + e.getMessage());
        }
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

    @PostMapping("/{loanId}/reject")
    public ResponseEntity<ApiResponseWrapper<Loan>> rejectedLoan(@PathVariable Long loanId) {
        ApiResponseWrapper<Loan> response = new ApiResponseWrapper<>();
        try {
            response.setData(loanHandler.rejectedLoan(loanId));
            response.setStatus(HttpStatus.OK.value());
            response.setMessage("Successfully approved Loan");
        } catch (Exception e) {
            response.setData(null);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.setMessage("Failed to reject Loan: " + e.getMessage());
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

}
