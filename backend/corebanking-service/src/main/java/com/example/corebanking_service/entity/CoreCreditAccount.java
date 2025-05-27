package com.example.corebanking_service.entity;

import com.example.common_service.constant.AccountType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@SuperBuilder
@Table(name = "credit_account")
public class CoreCreditAccount extends CoreAccount {

    @Column(name = "credit_limit", nullable = false)
    private BigDecimal creditLimit; // Hạn mức tín dụng tối đa ngân hàng cấp

    @Column(name = "current_debt", nullable = false)
    private BigDecimal currentDebt; // Số tiền mà khách đã sử dụng (nợ hiện tại)
    @ManyToOne
    @JoinColumn(name = "cart_type_id", nullable = false)
    private CoreCreditCardType coreCreditCardType;
    // Constructors
    public CoreCreditAccount() {
        super();
        this.setAccountType(AccountType.CREDIT); // Đặt loại tài khoản là CREDIT
        this.setBalance(BigDecimal.ZERO); // Tài khoản tín dụng không có balance thực, gán 0
        this.setCurrentDebt(BigDecimal.ZERO); // Mặc định nợ là 0 khi mới tạo
    }
}
