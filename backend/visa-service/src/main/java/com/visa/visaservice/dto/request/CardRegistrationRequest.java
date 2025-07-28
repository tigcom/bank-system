package com.visa.visaservice.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardRegistrationRequest {
    private String accountNumber;
    private String cifCode;
    private String cardType;
    private String customerName;
} 