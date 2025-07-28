package com.example.customer_service.responses;

import lombok.Data;

import java.util.List;

@Data
public class KycListResponse {
    private List<KycResponse> kycRequests;
    private long totalElements;
    private int totalPages;
    private int currentPage;
}
