package com.example.transaction_service.client;

import com.example.transaction_service.dto.response.ProviderDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;


@Component
@RequiredArgsConstructor
public class ProviderClient {

    @Autowired
    @Qualifier("mockServerRestTemplate")
    private RestTemplate mockServerRestTemplate;

    @Value("${mock-provider-api}")
    private String providerApiUrl;

    public Map<String, List<ProviderDTO>> getProviders() {
        String url = providerApiUrl + "/providers";

        try {
            ParameterizedTypeReference<Map<String, List<ProviderDTO>>> responseType =
                    new ParameterizedTypeReference<>() {};
            HttpEntity<String> entity = new HttpEntity<>(null);
            ResponseEntity<Map<String, List<ProviderDTO>>> responseEntity =
                    mockServerRestTemplate.exchange(url, HttpMethod.GET, entity, responseType);

            return responseEntity.getBody();

        } catch (RestClientException e) {
            // Trả về một map rỗng hoặc ném ra một exception tùy chỉnh
            return Collections.emptyMap();
        }
    }
}
