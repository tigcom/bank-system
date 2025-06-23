package com.example.corebanking_service.dto.resonse;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NapasInquiryResponse {
    private String accountName;
    private String status;
}
