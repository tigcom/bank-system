package com.example.customer_service.dtos;

import com.example.customer_service.models.KycStatus;
import lombok.Data;

@Data
public class ApproveKycRequest {
    private String cifCode;
    private KycStatus status; // VERIFIED hoặc REJECTED
    private String reason; // Lý do từ chối (nếu có)
}
