package com.example.common_service.dto.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data // Tự động tạo getters, setters, equals, hashCode, và toString
@NoArgsConstructor // Quan trọng: Tự động tạo constructor không đối số cho Jackson và JPA
@AllArgsConstructor // (Tùy chọn) Tự động tạo constructor với tất cả các trường, cũng hữu ích cho JPA
public class AccountSummaryDTO { // Đã thay đổi thành class
    private String accountNumber;
    private String cifCode;
    private String accountType;
    private BigDecimal balance;
    private BigDecimal initialDeposit;
    private Integer termValueMonths;
    private BigDecimal interestRate;
    private Boolean isActive;
    private BigDecimal creditLimit;
    private BigDecimal currentDebt;
    private BigDecimal annualFee;
    private String cardTypeName;
    private String cardImageUrl;
}