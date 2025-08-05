# TÀI LIỆU TỔNG HỢP THAY ĐỔI NGHIỆP VỤ & CODE LOAN-SERVICE

## Mục lục
1. [Logic Giải Ngân An Toàn](#logic-giải-ngân-an-toàn)
2. [Logic Tính Lãi Đúng Chuẩn](#logic-tính-lãi-đúng-chuẩn)
3. [Logic Đóng Khoản Vay Đúng](#logic-đóng-khoản-vay-đúng)
4. [Logic Xử Lý Trễ Hạn & Phạt](#logic-xử-lý-trễ-hạn--phạt)
5. [Tự Động Trừ Tiền Định Kỳ (Auto Deduct)](#tự-động-trừ-tiền-định-kỳ-auto-deduct)
6. [Kiểm Tra Số Dư Khi Thu Hồi](#kiểm-tra-số-dư-khi-thu-hồi)
7. [Các Công Thức & Quy Ước](#các-công-thức--quy-ước)
8. [Các Interface/Code Chính Đã Thêm](#các-interfacecode-chính-đã-thêm)
9. [Luồng Tổng Thể](#luồng-tổng-thể)

---

## 1. Logic Giải Ngân An Toàn
### Luồng xử lý
- Khi duyệt khoản vay, hệ thống **giải ngân trước** (chuyển tiền vào loan account).
- Chỉ khi giải ngân thành công mới chuyển trạng thái khoản vay sang APPROVED và tạo lịch trả nợ.
- Nếu giải ngân thất bại, rollback trạng thái và xóa lịch trả nợ.

```java
// LoanHandler.approveLoan
CommonTransactionDTO tx = commonTransactionService.loanDisbursement(disburseReq);
if (!"COMPLETED".equalsIgnoreCase(tx.getStatus())) {
    throw new IllegalArgumentException("Giải ngân thất bại: " + tx.getFailedReason());
}
loan = loanService.approveLoan(loan);
repaymentService.generateRepaymentSchedule(loan);
```

---

## 2. Logic Tính Lãi Đúng Chuẩn
### Công thức lãi suất dư nợ giảm dần
- **Lãi kỳ i:**  
  `interest_i = remaining_principal_i * monthly_interest_rate`
- **Gốc kỳ i:**  
  `principal_i = loan_amount / term_months` (kỳ cuối điều chỉnh để tổng gốc = loan_amount)

```java
BigDecimal monthlyInterestRate = loan.getInterestRate()
    .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
    .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
BigDecimal monthlyPrincipal = loan.getAmount()
    .divide(BigDecimal.valueOf(loan.getTermMonths()), 2, RoundingMode.HALF_UP);
BigDecimal remainingAmount = loan.getAmount();
for (int i = 0; i < loan.getTermMonths(); i++) {
    BigDecimal currentPrincipal = (i == loan.getTermMonths() - 1) ? remainingAmount : monthlyPrincipal;
    BigDecimal interest = remainingAmount.multiply(monthlyInterestRate).setScale(2, RoundingMode.HALF_UP);
    // ... tạo Repayment ...
    remainingAmount = remainingAmount.subtract(currentPrincipal);
}
```

---

## 3. Logic Đóng Khoản Vay Đúng
### Điều kiện đóng khoản vay
- Tất cả các kỳ trả nợ đã được thanh toán đủ (paidAmount >= principal + interest cho mọi kỳ).

```java
// RepaymentRepository.java
@Query("SELECT COUNT(r) > 0 FROM Repayment r WHERE r.loan.loanId = :loanId AND r.paidAmount < (r.principal + r.interest)")
boolean existsUnpaidRepaymentByLoanId(@Param("loanId") Long loanId);

// RepaymentServiceImpl.java
private Boolean shouldCloseLoan(Long loanId) {
    return !repaymentRepository.existsUnpaidRepaymentByLoanId(loanId);
}
```

---

## 4. Logic Xử Lý Trễ Hạn & Phạt
### Công thức phạt trễ hạn
- **Phạt = (gốc + lãi chưa trả) * 1.5%**

### Luồng xử lý
- Nếu kỳ trước cũng trễ hạn (UNPAID/PARTIAL) mà kỳ này vẫn trễ:
    - Đóng khoản vay, thu hồi toàn bộ số dư loan account về master account.
    - Nếu số dư = 0 thì chỉ đóng khoản vay, không thực hiện giao dịch thu hồi.
- Nếu chỉ trễ hạn 1 kỳ:
    - Tạo kỳ phạt mới (nếu là kỳ cuối) hoặc cộng dồn vào kỳ tiếp theo.

```java
BigDecimal unpaid = r.getPrincipal().add(r.getInterest()).subtract(r.getPaidAmount());
BigDecimal penalty = unpaid.multiply(BigDecimal.valueOf(0.015)).setScale(2, BigDecimal.ROUND_HALF_UP);
if (previousMonthLate) {
    // Đóng khoản vay, thu hồi nếu có tài sản
    BigDecimal loanBalance = getLoanAccountBalance(loan.getDisbursementAccountNumber());
    if (loanBalance.compareTo(BigDecimal.ZERO) > 0) {
        // Giao dịch thu hồi về master account
    }
} else {
    // Cộng dồn phạt vào kỳ tiếp theo hoặc tạo kỳ mới
}
```

---

## 5. Tự Động Trừ Tiền Định Kỳ (Auto Deduct)
### Luồng xử lý
- Scheduler chạy mỗi ngày, kiểm tra các khoản vay đến hạn trả nợ.
- Nếu repayment account đủ tiền, tự động trừ và cập nhật trạng thái PAID.
- Nếu không đủ tiền, đánh dấu LATE.

```java
@Scheduled(cron = "${repayment.scheduler.auto-deduct-cron:0 0 8 * * *}")
public void autoDeductRepayments() {
    for (Loan loan : loanService.getLoansApprove()) {
        List<Repayment> dueToday = getRepaymentsDueToday(loan.getLoanId());
        for (Repayment repayment : dueToday) {
            if (repayment.getStatus() == RepaymentStatus.UNPAID) {
                BigDecimal repaymentBalance = getRepaymentAccountBalance(loan.getRepaymentAccountNumber());
                BigDecimal requiredAmount = repayment.getPrincipal().add(repayment.getInterest());
                if (repaymentBalance.compareTo(requiredAmount) >= 0) {
                    performAutoDeduct(loan, repayment, requiredAmount);
                } else {
                    repayment.setStatus(RepaymentStatus.LATE);
                    repaymentService.updateRepayment(repayment);
                }
            }
        }
    }
}
```
**Giao diện chung:**
```java
// CommonTransactionService.java
CommonTransactionDTO autoDeductRepayment(AutoDeductRepaymentRequest autoDeductRequest);
```
**Triển khai ở transaction-service:**
```java
public TransactionDTO autoDeductRepayment(AutoDeductRequest autoDeductRequest) {
    // Tạo transaction type LOAN_PAYMENT, validate, process, save
}
```

---

## 6. Kiểm Tra Số Dư Khi Thu Hồi
- Nếu số dư loan account = 0 thì không thực hiện giao dịch thu hồi, chỉ đóng khoản vay.

---

## 7. Các Công Thức & Quy Ước
- **Lãi suất dư nợ giảm dần:**  
  `interest_i = remaining_principal_i * monthly_interest_rate`
- **Phạt trễ hạn:**  
  `penalty = (principal + interest - paidAmount) * 1.5%`
- **Đóng khoản vay:**  
  Khi tất cả các kỳ trả nợ đã PAID.

---

## 8. Các Interface/Code Chính Đã Thêm
- `CommonTransactionService.loanRecovery(CommonDisburseRequest)`
- `CommonTransactionService.autoDeductRepayment(AutoDeductRepaymentRequest)`
- DTO: `AutoDeductRepaymentRequest`, `AutoDeductRequest`
- Scheduler: `autoDeductRepayments()` trong `RepaymentCheckScheduler`
- JPA: `existsUnpaidRepaymentByLoanId` kiểm tra điều kiện đóng khoản vay

---

## 9. Luồng Tổng Thể
1. **Đăng ký vay:** Kiểm tra CIC, giải ngân an toàn, approve, tạo lịch trả nợ.
2. **Đến kỳ hạn:** Scheduler tự động trừ tiền từ repayment account.
3. **Nếu không đủ tiền:** Đánh dấu LATE, cộng dồn phạt vào kỳ tiếp theo.
4. **Nếu trễ hạn liên tiếp:** Đóng khoản vay, thu hồi số dư loan account về master account.
5. **Kết thúc khoản vay:** Khi tất cả các kỳ đã trả đủ.

---

**Tài liệu này tổng hợp đầy đủ các thay đổi nghiệp vụ, công thức, luồng xử lý và code mẫu để bạn có thể review, training hoặc bàn giao cho team khác. Nếu cần chi tiết về bất kỳ đoạn code nào, vui lòng hỏi thêm!** 