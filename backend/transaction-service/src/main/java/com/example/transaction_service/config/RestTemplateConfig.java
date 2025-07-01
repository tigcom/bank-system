package com.example.transaction_service.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Value("${core-banking.api-key}")
    private String CORE_BANK_API_KEY;

    @Value("${mock-server-api-key}")
    private String MOCK_SERVER_API_KEY;

    @Bean
    @Qualifier("coreBankRestTemplate")
    public RestTemplate coreBankRestTemplate() {
        return buildRestTemplateWithKey(CORE_BANK_API_KEY);
    }

    @Bean
    @Qualifier("mockServerRestTemplate")
    public RestTemplate mockServerRestTemplate() {
        return buildRestTemplateWithKey(MOCK_SERVER_API_KEY);
    }

    private RestTemplate buildRestTemplateWithKey(String apiKey) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().add("X-API-KEY", apiKey);
            return execution.execute(request, body);
        });
        return restTemplate;
    }
}
