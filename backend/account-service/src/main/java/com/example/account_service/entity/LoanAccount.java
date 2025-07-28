package com.example.account_service.entity;

import com.example.common_service.constant.AccountType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "loan_accounts")
public class LoanAccount extends Account {

    @Column(name = "loan_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal loanAmount; // Tổng tiền vay

    @Column(name = "outstanding_debt", nullable = false, precision = 19, scale = 2)
    private BigDecimal outstandingDebt; // Dư nợ còn lại

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 3)
    private BigDecimal interestRate; // Lãi suất vay

    @Column(name = "due_date")
    private LocalDate dueDate; // Ngày đáo hạn

    @PostLoad
    @PrePersist
    private void setDefaultAccountType() {
        if (this.getAccountType() == null) {
            this.setAccountType(AccountType.LOAN);
        }
        if (this.outstandingDebt == null && this.loanAmount != null) {
            this.outstandingDebt = loanAmount;
        }
    }
}