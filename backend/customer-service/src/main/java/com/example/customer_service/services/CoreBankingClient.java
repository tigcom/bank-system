package com.example.customer_service.services;

import com.example.common_service.dto.customer.CoreCustomerDTO;
import com.example.common_service.dto.customer.CoreResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class CoreBankingClient {
    @Autowired
    @Qualifier("coreBankingRestTemplate")
    private  RestTemplate restTemplate;

    public CoreResponse syncCustomer(CoreCustomerDTO dto) {
        ResponseEntity<CoreResponse> response = restTemplate.postForEntity(
                "http://localhost:8083/corebanking/api/core/customers/sync",
                dto,
                CoreResponse.class
        );
        return response.getBody();
    }
}

