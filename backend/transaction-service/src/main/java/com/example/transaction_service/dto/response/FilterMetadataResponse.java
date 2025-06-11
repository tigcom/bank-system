package com.example.transaction_service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class FilterMetadataResponse {
    List<FilterOptionDTO> transactionTypes;
    List<FilterOptionDTO> statuses;
    List<FilterOptionDTO> currencies;
}
