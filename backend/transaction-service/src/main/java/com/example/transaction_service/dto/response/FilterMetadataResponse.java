package com.example.transaction_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilterMetadataResponse {
    List<FilterOptionDTO> transactionTypes;
    List<FilterOptionDTO> statuses;
    List<FilterOptionDTO> currencies;
}
