package com.example.common_service.dto.response;

import com.example.common_service.constant.AccountStatus;
import com.example.common_service.constant.AccountType;
import com.example.common_service.constant.InterestPaymentType;
import com.example.common_service.constant.RenewOption;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data // Tự động tạo getters, setters, equals, hashCode, và toString
@NoArgsConstructor // Quan trọng: Tự động tạo constructor không đối số cho Jackson và JPA
@AllArgsConstructor // (Tùy chọn) Tự động tạo constructor với tất cả các trường, cũng hữu ích cho JPA
@Builder
public class AccountSummaryDTO { // Đã thay đổi thành class
    private String accountNumber;
    private String cifCode;
    private AccountType accountType;
    private BigDecimal balance;
    private AccountStatus status;
    private LocalDate openedDate;
    private BigDecimal interestRate;
    private BigDecimal initialDeposit;
    private Integer termValueMonths;
    
    // Credit specific fields
    private BigDecimal creditLimit;
    private BigDecimal currentDebt;
    private String creditCardType;
    
    // Savings specific fields  
    private LocalDateTime maturityDate;
    private InterestPaymentType interestPaymentType;
    private RenewOption renewOption;
}