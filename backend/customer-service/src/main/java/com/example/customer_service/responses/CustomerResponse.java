package com.example.customer_service.responses;

import com.example.common_service.constant.CustomerStatus;
import com.example.customer_service.models.KycStatus;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

@Data
public class CustomerResponse  implements Serializable {
    private static final long serialVersionUID = 1L;
    private String cifCode;
    private String fullName;
    private String address;
    private String email;
    private LocalDate dateOfBirth;
    private String phoneNumber;
    private CustomerStatus status;
    private KycStatus kycStatus;
    private String identityNumber;

}
