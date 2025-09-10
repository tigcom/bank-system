package com.example.account_service.dto.request;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder

public class PaymentRequest implements Serializable {
    private String cifCode;
}
