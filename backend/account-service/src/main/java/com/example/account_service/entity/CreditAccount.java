package com.example.account_service.entity;

import com.example.common_service.constant.AccountType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(name = "credit_accounts")
public class CreditAccount extends Account {

    @Column(name = "credit_limit", nullable = false, precision = 19, scale = 2)
    private BigDecimal creditLimit; // Hạn mức tín dụng tối đa ngân hàng cấp

    @Column(name = "current_debt", nullable = false, precision = 19, scale = 2)
    private BigDecimal currentDebt = BigDecimal.ZERO; // Số tiền mà khách đã sử dụng (nợ hiện tại)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_type_id", nullable = false)
    private CreditCardType creditCardType;

    @Column(name="expiry_Date")
    private LocalDate expiryDate; // Ngày hết hạn thẻ

    @Column(name="card_Token")
    private String cardToken; // Token thẻ cho giao dịch

    @Column(name="card_number")
    private String cardNumber;

    @Column(name="card_holder_name")
    private String cardHolderName;
    @PostLoad
    @PrePersist
    private void initDefaults() {
        if (this.getAccountType() == null) {
            this.setAccountType(AccountType.CREDIT);

        }
        if (this.getCurrentDebt() == null) {
            this.setCurrentDebt(BigDecimal.ZERO);
        }
    }
}