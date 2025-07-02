package com.example.loan_service.restcontroller;

import com.example.common_service.dto.CustomerResponseDTO;
import com.example.common_service.dto.response.AccountPaymentResponse;
import com.example.loan_service.dto.request.LoanRejectionReasonRequestDTO;
import com.example.loan_service.dto.request.LoanRequestDTO;
import com.example.loan_service.entity.Loan;
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
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanHandler loanHandler;

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

    @PreAuthorize("hasRole('ADMIN')")
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

}
