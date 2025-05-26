package com.example.corebanking_service.entity;

import com.example.corebanking_service.constant.AccountType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "credit_account")
public class CoreCreditAccount extends CoreAccount {

    @Column(name = "credit_limit", nullable = false)
    private Long creditLimit; // Hạn mức tín dụng tối đa ngân hàng cấp

    @Column(name = "current_debt", nullable = false)
    private Long currentDebt; // Số tiền mà khách đã sử dụng (nợ hiện tại)

    // Constructors
    public CoreCreditAccount() {
        super();
        this.setAccountType(AccountType.CREDIT); // Đặt loại tài khoản là CREDIT
        this.setBalance(0L); // Tài khoản tín dụng không có balance thực, gán 0
        this.setCurrentDebt(0L); // Mặc định nợ là 0 khi mới tạo
    }
}
