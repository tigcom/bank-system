package com.example.common_service.dto.response;

import com.example.common_service.constant.CustomerStatus;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@Builder
public class CustomerResponse implements Serializable {
    private String userId;
    private String cifCode;
    private String fullName;
    private String address;
    private String email;
    private String identityNumber;
    private LocalDate dateOfBirth;
    private String phoneNumber;
    private CustomerStatus status;

}
