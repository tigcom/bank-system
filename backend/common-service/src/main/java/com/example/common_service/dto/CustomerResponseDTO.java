package com.example.common_service.dto;

import com.example.common_service.constant.CustomerStatus;
import com.example.common_service.models.KycStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CustomerResponseDTO implements Serializable {
    private Long id;
    private String cifCode;
    private String fullName;
    private String address;
    private LocalDate dateOfBirth;
    private String email;
    private String phoneNumber;
    private CustomerStatus status;
}

