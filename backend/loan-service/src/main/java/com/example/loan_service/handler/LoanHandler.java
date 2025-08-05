package com.example.loan_service.handler;

import com.example.loan_service.models.LoanStatus;
import com.example.common_service.constant.LoanType;
import com.example.common_service.dto.*;
import com.example.common_service.constant.CustomerStatus;
import com.example.common_service.dto.request.*;
import com.example.common_service.dto.response.AccountPaymentResponse;
import com.example.common_service.services.CommonService;
import com.example.common_service.services.account.AccountDubboService;
import com.example.common_service.services.account.AccountQueryService;
import com.example.common_service.services.customer.CustomerCommonService;
import com.example.common_service.services.customer.CustomerQueryService;
import com.example.common_service.services.transactions.CommonTransactionService;
import com.example.loan_service.dto.request.InfoIncomeRequestDto;
import com.example.loan_service.dto.request.LoanRejectionReasonRequestDTO;
import com.example.common_service.dto.request.LoanRequestDTO;
import com.example.loan_service.dto.response.CicResponse;
import com.example.loan_service.dto.response.TransactionDto;
import com.example.loan_service.entity.InfoIncome;
import com.example.loan_service.entity.Loan;
import com.example.loan_service.entity.LoanRejectionReason;
import com.example.loan_service.entity.Repayment;
import com.example.loan_service.handler.LoanHandler;
import com.example.loan_service.mapper.CoreAccountMapper;
import com.example.loan_service.mapper.InfoIncomeMapper;
import com.example.loan_service.mapper.LoanMapper;
import com.example.loan_service.mapper.RepaymentMapper;
import com.example.loan_service.models.RepaymentStatus;
import com.example.loan_service.response.ApiResponseWrapper;
import com.example.loan_service.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.rpc.RpcContext;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoanHandler {
    private final StreamBridge streamBridge;
    private final LoanService loanService;
    private final LoanMapper loanMapper;
    private final InfoIncomeMapper  infoIncomeMapper;
    private final CoreBankingClient coreBankingClient;
    private final RepaymentService repaymentService;
    private final CICClient cicClient;
    private final  OpeningBankingClient openingBankingClient;
    private final InfoIncomeService infoIncomeService;
    private final LoanRejectionReasonService loanRejectionReasonService;
    @DubboReference private final CustomerQueryService customerQueryService;
    @DubboReference private final AccountQueryService accountQueryService;
    @DubboReference private final CommonTransactionService commonTransactionService;
    @DubboReference private final CommonService commonService;
    @DubboReference private final AccountDubboService accountDubboService;
    @DubboReference private final CustomerCommonService customerCommonService;

    public Loan approveLoan(Long loanId) {
        log.info("APPROVE_LOAN_HANDLER_START - loanId: {}", loanId);
        Loan loan = null;
        try {
            loan = loanService.getLoanById(loanId)
                    .orElseThrow(() -> new IllegalArgumentException("Loan not found: " + loanId));
            log.debug("LOAN_FETCHED - {}", loan);

            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            RpcContext.getClientAttachment().setAttachment("username", username);
            log.info("user name: {}", username);
            
            // Bước 1: Tạo tài khoản vay
            LoanRequestDTO dto = new LoanRequestDTO();
            dto.setDisbursementAccountNumber(loan.getDisbursementAccountNumber());
            dto.setRepaymentAccountNumber(loan.getRepaymentAccountNumber());
            dto.setAmount(loan.getAmount());
            dto.setInterestRate(loan.getInterestRate());
            dto.setTermMonths(loan.getTermMonths());
            dto.setCustomerId(loan.getCustomerId());
            dto.setCreatedAt(LocalDateTime.now());
            dto.setStatus(com.example.common_service.constant.LoanStatus.APPROVED);
            dto.setLoanType(loan.getLoanType());
            
            AccountDTO accountDTO = accountDubboService.createLoanAccount(dto);
            log.info("accountDto : {}",accountDTO.toString());
            loan.setDisbursementAccountNumber(accountDTO.getAccountNumber());
            
            // Bước 2: Thực hiện giải ngân trước khi approve
            CommonDisburseRequest disburseReq = new CommonDisburseRequest();
            disburseReq.setToAccountNumber(loan.getDisbursementAccountNumber());
            disburseReq.setAmount(loan.getAmount());
            disburseReq.setCurrency("VND");
            RpcContext.getClientAttachment().setAttachment("username", username);
            CommonTransactionDTO tx = commonTransactionService.loanDisbursement(disburseReq);
            log.info("LOAN_DISBURSE_TRANSACTION - status: {}, ref: {}", tx.getStatus(), tx.getReferenceCode());
            
            if (!"COMPLETED".equalsIgnoreCase(tx.getStatus())) {
                log.error("APPROVE_LOAN_DISBURSE_FAILED - reason: {}", tx.getFailedReason());
                throw new IllegalArgumentException("Giải ngân thất bại: " + tx.getFailedReason());
            }
            
            // Bước 3: Chỉ approve và tạo lịch trả nợ sau khi giải ngân thành công
            loan = loanService.approveLoan(loan);
            repaymentService.generateRepaymentSchedule(loan);

            log.info("LOAN_APPROVED_AND_SYNCED - loanId: {}", loanId);
            
            // Bước 4: Gửi thông báo
            CustomerResponseDTO customer = customerQueryService.getCustomerById(loan.getCustomerId());
            MailMessageDTO mail = new MailMessageDTO();
            mail.setSubject("KÍCH HOẠT KHOẢN VAY");
            mail.setRecipient("phanhuynhphuckhang12c8@gmail.com");
            mail.setBody("Khoản vay đã duyệt và giải ngân tài khoản: " + loan.getDisbursementAccountNumber());
            mail.setRecipientName(customer.getFullName());
            streamBridge.send("mail-out-0", mail);
            log.info("APPROVE_LOAN_MAIL_SENT - loanId: {}, to: {}", loanId, customer.getEmail());
            log.info("APPROVE_LOAN_HANDLER_SUCCESS - loanId: {}", loanId);
            return loan;
            
        } catch (IllegalArgumentException e) {
            log.error("APPROVE_LOAN_HANDLER_INVALID - loanId: {}, error: {}", loanId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("APPROVE_LOAN_HANDLER_ERROR - loanId: {}, error: {}", loanId, e.getMessage(), e);
            // Nếu có lỗi sau khi đã approve, cần rollback
            if (loan != null && LoanStatus.APPROVED.equals(loan.getStatus())) {
                try {
                    log.warn("ROLLBACK_LOAN_APPROVAL - loanId: {}", loanId);
                    loan.setStatus(LoanStatus.PENDING);
                    loanService.updateLoan(loan);
                    // Xóa lịch trả nợ đã tạo
                    repaymentService.deleteRepaymentsByLoanId(loanId);
                } catch (Exception rollbackEx) {
                    log.error("ROLLBACK_LOAN_APPROVAL_FAILED - loanId: {}, error: {}", loanId, rollbackEx.getMessage());
                }
            }
            throw e;
        }
    }
    public Loan createLoan(LoanRequestDTO dto)  {
        log.info("CREATE_LOAN_HANDLER_START - request: {}", dto);
        try {
            Long customerId = getCustomerId();
            log.debug("CUSTOMER_ID_FETCHED - {}", customerId);
            CustomerResponseDTO customer = customerQueryService.getCustomerById(customerId);
            log.info("CUSTOMER_INFO - idNumber={}, status={}", customer.getIdentityNumber(), customer.getStatus());
            AccountDTO repaymentAccount = accountQueryService.getAccountByAccountNumber(dto.getRepaymentAccountNumber());
            log.info("ACCOUNT_INFO - repaymentAccount={}, status={}", dto.getRepaymentAccountNumber(), repaymentAccount.getStatus());
            if (!CustomerStatus.ACTIVE.equals(customer.getStatus())) {
                throw new IllegalArgumentException("Hồ sơ khách hàng không hợp lệ");
            }
            if (Period.between(customer.getDateOfBirth(), LocalDate.now()).getYears() <= 18) {
                throw new IllegalArgumentException("Người dùng chưa đủ tuổi");
            }
            if (!"ACTIVE".equalsIgnoreCase(repaymentAccount.getStatus())) {
                throw new IllegalArgumentException("Tài khoản thanh toán không hợp lệ");
            }
            CICRequest cicRequest = new CICRequest();
            cicRequest.setIdNumber(customer.getIdentityNumber());
            cicRequest.setName(customer.getFullName());
            log.info("CALLING_CIC - CICRequest: {}", cicRequest);
            CicResponse cicResponse = cicClient.checkCIC(cicRequest);
            log.info("CIC_RESPONSE - status={}, creditScore={}, overdue={}, debtGroup={}, errorCode={}, message={}",
                    cicResponse.getStatus(),
                    cicResponse.getCreditScore(),
                    cicResponse.getOverdue(),
                    cicResponse.getDebtGroup(),
                    cicResponse.getErrorCode(),
                    cicResponse.getMessage()
            );
            if (!"success".equalsIgnoreCase(cicResponse.getStatus())) throw new IllegalArgumentException("Không thể truy vấn CIC: " + cicResponse.getMessage());
            int score = cicResponse.getCreditScore();
            boolean overdue = cicResponse.getOverdue();
            int group = cicResponse.getDebtGroup();
            if (overdue)  throw new IllegalArgumentException("Khách hàng đang có nợ quá hạn theo CIC");
            if (group >= 2)   throw new IllegalArgumentException("Khách hàng thuộc nhóm nợ xấu (nhóm " + group + ")");
            if (score < 700 || group == 1)  log.warn("CIC warning: Khách hàng có điểm tín dụng trung bình hoặc nhóm nợ cần chú ý");
            log.info("CIC PASS - Khách hàng đủ điều kiện tín dụng");
            Loan l = loanMapper.toEntity(dto);
            l.setCustomerId(customerId);
            l.setCreatedAt(LocalDateTime.now());
            Loan r = loanService.createLoan(l);
            log.info("CREATE_LOAN_HANDLER_SUCCESS - loanId: {}", l.getLoanId());
            return r;
        } catch (IllegalArgumentException e) {
            log.error("CREATE_LOAN_HANDLER_INVALID - dto={}, error: {}", dto, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("CREATE_LOAN_HANDLER_ERROR - dto={}, error: {}", dto, e.getMessage(), e);
            throw new RuntimeException("Xảy ra lỗi khi xử lý khoản vay", e);
        }
    }
    public Loan updateLoan(LoanRequestDTO dto) {
        log.info("UPDATE_LOAN_HANDLER_START - request: {}", dto);
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        RpcContext.getClientAttachment().setAttachment("username", username);
        try {
            Loan loan = loanMapper.toEntity(dto);
            Loan updatedLoan = loanService.updateLoan(loan);
            accountDubboService.updateAccountFromLoan(dto);
            CoreAccountRequest coreAccountRequest = CoreAccountMapper.INSTANCE.fromLoan(loan);
            coreBankingClient.updateAccount(coreAccountRequest);
            log.info("UPDATE_LOAN_HANDLER_SUCCESS - loanId: {}", updatedLoan.getLoanId());
            return updatedLoan;
        } catch (Exception e) {
            log.error("UPDATE_LOAN_HANDLER_ERROR - error: {}", e.getMessage(), e);
            throw e;
        }
    }
    public Optional<Loan> getLoanById(Long loanId) {
        log.info("GET_LOAN_BY_ID_HANDLER_START - loanId: {}", loanId);
        try {
            Optional<Loan> loan = loanService.getLoanById(loanId);
            log.info("GET_LOAN_BY_ID_HANDLER_SUCCESS - found: {}", loan.isPresent());
            return loan;
        } catch (Exception e) {
            log.error("GET_LOAN_BY_ID_HANDLER_ERROR - loanId: {}, error: {}", loanId, e.getMessage(), e);
            throw e;
        }
    }
    public List<Loan> getLoansByCustomerId() {
        log.info("GET_LOANS_BY_CUSTOMER_HANDLER_START");
        try {
            Long customerId = getCustomerId();
            List<Loan> list = loanService.getLoansByCustomerId(customerId);
            log.info("GET_LOANS_BY_CUSTOMER_HANDLER_SUCCESS - count: {}", list.size());
            return list;
        } catch (Exception e) {
            log.error("GET_LOANS_BY_CUSTOMER_HANDLER_ERROR - error: {}", e.getMessage(), e);
            throw e;
        }
    }
    public Loan closedLoan(Long loanId) {
        log.info("CLOSE_LOAN_HANDLER_START - loanId: {}", loanId);
        try {
            Loan loan = loanService.closedLoan(loanId);
            com.example.common_service.dto.CoreAccountRequest coreAccountRequest = com.example.loan_service.mapper.CoreAccountMapper.INSTANCE.fromLoan(loan);
            coreBankingClient.updateAccount(coreAccountRequest);
            log.info("CLOSE_LOAN_HANDLER_SUCCESS - loanId: {}", loanId);
            return loan;
        } catch (Exception e) {
            log.error("CLOSE_LOAN_HANDLER_ERROR - loanId: {}, error: {}", loanId, e.getMessage(), e);
            throw e;
        }
    }
    public Loan rejectedLoan(Long loanId, LoanRejectionReasonRequestDTO req) {
        log.info("REJECT_LOAN_HANDLER_START - loanId: {}, reason: {}", loanId, req.getReason());
        try {
            Loan loan = loanService.rejectedLoan(loanId);
            LoanRejectionReason reason = new LoanRejectionReason();
            reason.setReason(req.getReason());
            reason.setLoan(loan);
            loanRejectionReasonService.save(reason);
            log.info("REJECT_LOAN_HANDLER_SUCCESS - loanId: {}", loanId);
            return loan;
        } catch (Exception e) {
            log.error("REJECT_LOAN_HANDLER_ERROR - loanId: {}, error: {}", loanId, e.getMessage(), e);
            throw e;
        }
    }
    public List<Loan> findall() {
        log.info("FIND_ALL_LOANS_HANDLER_START");
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            JwtAuthenticationToken jwt = (JwtAuthenticationToken) auth;
//            log.info("+++++++++++++++++++++++++++++");
//            Long customerId = getCustomerId();
//            log.info("customerid: {}", customerId);
//            log.info("+++++++++++++++++++++++++++++");

            List<Loan> list = loanService.findAllLoan();
            log.info("FIND_ALL_LOANS_HANDLER_SUCCESS - count: {}", list.size());
            return list;
        } catch (Exception e) {
            log.error("FIND_ALL_LOANS_HANDLER_ERROR - error: {}", e.getMessage(), e);
            throw e;
        }
    }
    public void deleteLoan(Long loanId) {
        log.info("DELETE_LOAN_HANDLER_START - loanId: {}", loanId);
        try {
            loanService.deleteLoan(loanId);
            coreBankingClient.deleteLoan(loanId);
            log.info("DELETE_LOAN_HANDLER_SUCCESS - loanId: {}", loanId);
        } catch (Exception e) {
            log.error("DELETE_LOAN_HANDLER_ERROR - loanId: {}, error: {}", loanId, e.getMessage(), e);
            throw e;
        }
    }
    public List<Repayment> getRepaymentsByLoanId(Long loanId) {
        log.info("GET_REPAYMENTS_BY_LOAN_HANDLER_START - loanId: {}", loanId);
        try {
            List<Repayment> list = repaymentService.getRepaymentsByLoanId(loanId);
            log.info("GET_REPAYMENTS_BY_LOAN_HANDLER_SUCCESS - loanId: {}, count: {}", loanId, list.size());
            return list;
        } catch (Exception e) {
            log.error("GET_REPAYMENTS_BY_LOAN_HANDLER_ERROR - loanId: {}, error: {}", loanId, e.getMessage(), e);
            throw e;
        }
    }
    public String makeRepayment(Long repaymentId, BigDecimal amount, String accountNumber) {
        log.info("MAKE_REPAYMENT_HANDLER_START - repaymentId: {}, amount: {}, account: {}", repaymentId, amount, accountNumber);
        try {
            PayRepaymentRequest pay = new PayRepaymentRequest();
            pay.setAmount(amount);
            pay.setCurrency("VND");
            pay.setDescription("OTP request");
            pay.setFromAccountNumber(accountNumber);

            CommonTransactionDTO tx = commonTransactionService.loanPayment(pay);
            log.info("MAKE_REPAYMENT_TRANSACTION - status: {}, ref: {}", tx.getStatus(), tx.getReferenceCode());
            if (!"PENDING".equalsIgnoreCase(tx.getStatus())) {
                throw new IllegalArgumentException(tx.getFailedReason());
            }
            log.info("MAKE_REPAYMENT_HANDLER_SUCCESS - referenceCode: {}", tx.getReferenceCode());
            return tx.getReferenceCode();
        } catch (IllegalArgumentException e) {
            log.error("MAKE_REPAYMENT_HANDLER_INVALID - {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("MAKE_REPAYMENT_HANDLER_ERROR - error: {}", e.getMessage(), e);
            throw e;
        }
    }
    public Repayment confirmRepayment(Long repaymentId, BigDecimal amount, String otpCode, String referenceCode) {
        log.info("CONFIRM_REPAYMENT_HANDLER_START - repaymentId: {}, referenceCode: {}", repaymentId, referenceCode);
        try {
            CommonConfirmTransactionRequest confirm = new CommonConfirmTransactionRequest();
            confirm.setOtpCode(otpCode);
            confirm.setReferenceCode(referenceCode);

            CommonTransactionDTO tx = commonTransactionService.confirmTransaction(confirm);
            log.info("CONFIRM_REPAYMENT_TRANSACTION - status: {}", tx.getStatus());
            if (!"COMPLETED".equalsIgnoreCase(tx.getStatus())) {
                throw new IllegalArgumentException(tx.getFailedReason());
            }

            Repayment r = repaymentService.makeRepayment(repaymentId, amount);
            // Gọi updateAccountFromLoan với paidAmount
            try {
                Loan loan = r.getLoan();
                LoanRequestDTO updateDto = new LoanRequestDTO();
                updateDto.setLoanId(loan.getLoanId());
                updateDto.setAmount(loan.getAmount());
                updateDto.setInterestRate(loan.getInterestRate());
                updateDto.setDisbursementAccountNumber(loan.getDisbursementAccountNumber());
                updateDto.setTermMonths(loan.getTermMonths());
                updateDto.setPaidAmount(r.getPaidAmount());
                // Đồng bộ corebanking

                if (repaymentService.shouldCloseLoan(loan.getLoanId())) {
                    log.info("CLOSE_LOAN_AFTER_REPAYMENT - loanId: {}", loan.getLoanId());
                    loanService.closedLoan(loan.getLoanId());
                    updateDto.setStatus(com.example.common_service.constant.LoanStatus.CLOSED);
                    loan.setStatus(LoanStatus.CLOSED);
                }
                CoreAccountRequest coreAccountRequest = CoreAccountMapper.INSTANCE.fromLoan(loan);
                coreAccountRequest.setBalance(repaymentService.getOutstandingDebtByLoanId(loan.getLoanId()));
                log.warn("UPDATE_ACCOUNT_core: {}",repaymentService.getOutstandingDebtByLoanId(loan.getLoanId()));
                accountDubboService.updateAccountFromLoan(updateDto);
                coreBankingClient.updateAccount(coreAccountRequest);
            } catch (Exception ex) {
                log.warn("UPDATE_ACCOUNT_FROM_LOAN_AFTER_REPAYMENT_FAILED: {}", ex.getMessage());
            }
            log.info("CONFIRM_REPAYMENT_HANDLER_SUCCESS - repaymentId: {}", repaymentId);
            return r;
        } catch (IllegalArgumentException e) {
            log.error("CONFIRM_REPAYMENT_HANDLER_INVALID - {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("CONFIRM_REPAYMENT_HANDLER_ERROR - error: {}", e.getMessage(), e);
            throw e;
        }
    }
    public List<Repayment> getHistory() {
        log.info("GET_HISTORY_HANDLER_START");
        try {
            Long customerId = getCustomerId();
            List<Repayment> history = repaymentService.getHistoryRepayment(customerId);
            log.info("GET_HISTORY_HANDLER_SUCCESS - count: {}", history.size());
            return history;
        } catch (Exception e) {
            log.error("GET_HISTORY_HANDLER_ERROR - error: {}", e.getMessage(), e);
            throw e;
        }
    }
    public Repayment getCurrentRepayment() {
        log.info("GET_CURRENT_REPAYMENT_HANDLER_START");
        try {
            Long customerId = getCustomerId();
            Repayment r = repaymentService.getCurrentRepayment(customerId);
            log.info("GET_CURRENT_REPAYMENT_HANDLER_SUCCESS - repaymentId: {}", r.getRepaymentId());
            return r;
        } catch (Exception e) {
            log.error("GET_CURRENT_REPAYMENT_HANDLER_ERROR - error: {}", e.getMessage(), e);
            throw e;
        }
    }
    public Optional<Repayment> getRepaymentById(Long repaymentId) {
        log.info("GET_REPAYMENT_BY_ID_HANDLER_START - repaymentId: {}", repaymentId);
        try {
            Optional<Repayment> r = repaymentService.getRepaymentById(repaymentId);
            log.info("GET_REPAYMENT_BY_ID_HANDLER_SUCCESS - found: {}", r.isPresent());
            return r;
        } catch (Exception e) {
            log.error("GET_REPAYMENT_BY_ID_HANDLER_ERROR - error: {}", e.getMessage(), e);
            throw e;
        }
    }
    public Repayment unpaidRepayment(Long repaymentId) {
        log.info("UNPAID_REPAYMENT_HANDLER_START - repaymentId: {}", repaymentId);
        try {
            Repayment r = repaymentService.updateRepaymentStatus(repaymentId, RepaymentStatus.UNPAID);
            log.info("UNPAID_REPAYMENT_HANDLER_SUCCESS - repaymentId: {}", repaymentId);
            return r;
        } catch (Exception e) {
            log.error("UNPAID_REPAYMENT_HANDLER_ERROR - error: {}", e.getMessage(), e);
            throw e;
        }
    }
    public Repayment lateRepayment(Long repaymentId) {
        log.info("LATE_REPAYMENT_HANDLER_START - repaymentId: {}", repaymentId);
        try {
            Repayment r = repaymentService.updateRepaymentStatus(repaymentId, RepaymentStatus.LATE);
            log.info("LATE_REPAYMENT_HANDLER_SUCCESS - repaymentId: {}", repaymentId);
            return r;
        } catch (Exception e) {
            log.error("LATE_REPAYMENT_HANDLER_ERROR - error: {}", e.getMessage(), e);
            throw e;
        }
    }
    public BigDecimal getTotalBorrowed() {
        log.info("GET_TOTAL_BORROWED_HANDLER_START");
        try {
            Long customerId = getCustomerId();
            BigDecimal total = loanService.getLoansApproveAndCustomerId(customerId)
                    .stream().map(Loan::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            log.info("GET_TOTAL_BORROWED_HANDLER_SUCCESS - total: {}", total);
            return total;
        } catch (Exception e) {
            log.error("GET_TOTAL_BORROWED_HANDLER_ERROR - error: {}", e.getMessage(), e);
            throw e;
        }
    }
    public BigDecimal getTotalOutstanding() {
        log.info("GET_TOTAL_OUTSTANDING_HANDLER_START");
        try {
            Long customerId = getCustomerId();
            BigDecimal total = loanService.getLoansApproveAndCustomerId(customerId).stream()
                .flatMap(l -> l.getRepayments().stream())
                .map(r -> r.getPrincipal().add(r.getInterest()).subtract(r.getPaidAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            log.info("GET_TOTAL_OUTSTANDING_HANDLER_SUCCESS - total: {}", total);
            return total;
        } catch (Exception e) {
            log.error("GET_TOTAL_OUTSTANDING_HANDLER_ERROR - error: {}", e.getMessage(), e);
            throw e;
        }
    }
    public Long getCustomerId() {
        log.info("GET_CUSTOMER_ID_HANDLER_START");
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            JwtAuthenticationToken jwt = (JwtAuthenticationToken) auth;
            String user = jwt.getName();
            Long customerId = commonService.getCurrentCustomer(user).getCustomerId();
            log.info("GET_CUSTOMER_ID_HANDLER_SUCCESS - customerId: {}", customerId);
            return customerId;
        } catch (Exception e) {
            log.error("GET_CUSTOMER_ID_HANDLER_ERROR - error: {}", e.getMessage(), e);
            throw e;
        }
    }
    public void deleteRepaymentsByLoanId(Long loanId) {
        log.info("DELETE_REPAYMENTS_BY_LOAN_HANDLER_START - loanId: {}", loanId);
        try {
            repaymentService.deleteRepaymentsByLoanId(loanId);
            log.info("DELETE_REPAYMENTS_BY_LOAN_HANDLER_SUCCESS - loanId: {}", loanId);
        } catch (Exception e) {
            log.error("DELETE_REPAYMENTS_BY_LOAN_HANDLER_ERROR - loanId: {}, error: {}", loanId, e.getMessage(), e);
            throw e;
        }
    }
    public CustomerResponseDTO getCustomerDetailById(Long id) {
        try {
            log.debug("CUSTOMER_ID_FETCHED - {}", id);
            CustomerResponseDTO customer = customerQueryService.getCustomerById(id);
            log.info("CUSTOMER_INFO - status: {}", customer.getStatus());
            return customer;
        } catch (Exception e) {
            log.error("CUSTOMER_ID_FETCHED - error: {}", e.getMessage(), e);
            throw e;
        }
    }


    public List<AccountPaymentResponse> getAllPaymentAccountsbyUserId(String id) {
        try {
            log.debug("GET_ALL_PAYMENT_ACCOUNTS_BY_USER_START - userId: {}", id);
            List<AccountPaymentResponse> accounts = customerCommonService.getAllPaymentAccountsbyUserId(id);
            log.info("GET_ALL_PAYMENT_ACCOUNTS_BY_USER_SUCCESS - userId: {}, totalAccounts: {}", id, accounts.size());
            return accounts;
        } catch (Exception e) {
            log.error("GET_ALL_PAYMENT_ACCOUNTS_BY_USER_ERROR - userId: {}, error: {}", id, e.getMessage(), e);
            throw e;
        }
    }
    public List<AccountPaymentResponse> getAllPaymentAccountsbyCurrentUser() {
        try {
            log.debug("GET_ALL_PAYMENT_ACCOUNTS_BY_CURRENT_USER");
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            JwtAuthenticationToken jwt = (JwtAuthenticationToken) auth;
            String id = jwt.getName();
            log.info("GET_USER_ID_FROM_AUTHEN - userId: {}", id);
            List<AccountPaymentResponse> accounts = customerCommonService.getAllPaymentAccountsbyUserId(id);
            log.info("GET_ALL_PAYMENT_ACCOUNTS_BY_USER_SUCCESS - userId: {}, totalAccounts: {}", id, accounts.size());
            return accounts;
        } catch (Exception e) {
            log.error("GET_ALL_PAYMENT_ACCOUNTS_BY_USER_ERROR - userId: {}, error: {}", null, e.getMessage(), e);
            throw e;
        }
    }
    public List<TransactionDto> checkInfoIncome(InfoIncomeRequestDto infoIncome) {
        log.info("LOAN_HANDLER_CHECK_INFO_INCOME input: {}", infoIncome);
        try {
            List<TransactionDto> result = openingBankingClient.checkIncome(infoIncome);
            log.info("LOAN_HANDLER_CHECK_INFO_INCOME_SUCCESS size: {}", result.size());
            return result;
        } catch (Exception e) {
            log.error("LOAN_HANDLER_CHECK_INFO_INCOME_ERROR: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public java.math.BigDecimal getTotalDisbursedSystem() {
        return loanService.getTotalDisbursedSystem();
    }
    public java.math.BigDecimal getTotalCollectedSystem() {
        return repaymentService.getTotalCollectedSystem();
    }
    public java.math.BigDecimal getTotalProfitSystem() {
        return repaymentService.getTotalProfitSystem();
    }
    public Map<String, Long> getRepaymentStats() {
        return repaymentService.getRepaymentStats();
    }
}
