package com.example.common_service.dto;

import com.example.common_service.constant.AccountType;
import com.example.common_service.constant.CustomerStatus;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class CustomerDTO implements  Serializable {
    private Long customerId;
    private String userId;
    private String cifCode;
    private String username;
    private String fullName;
    private String email;
    private CustomerStatus status;
}
