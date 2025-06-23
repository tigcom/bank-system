package com.example.common_service.services.customer;

import com.example.common_service.dto.AccountDTO;

import java.util.List;

public interface CustomerCommonService {
    List<AccountDTO> getAccountsByCifCode(String cifCode);
}


