package com.example.transaction_service.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FilterOptionDTO {
    private String label;
    private String value;
}
