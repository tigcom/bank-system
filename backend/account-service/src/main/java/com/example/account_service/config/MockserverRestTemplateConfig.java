package com.example.account_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Configuration(proxyBeanMethods = false)
public class MockserverRestTemplateConfig {
    @Value("${core-banking.api.key}")
    private String mockAPIKey;

    @Bean(name = "MockServerRestTemplate")
    public RestTemplate mockRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        // Interceptor thêm API Key vào header
        ClientHttpRequestInterceptor apiKeyInterceptor = (request, body, execution) -> {
            request.getHeaders().add("Mock-API-Key", mockAPIKey); // tuỳ hệ thống có thể là "x-api-key", "Authorization", v.v.
            return execution.execute(request, body);
        };
        restTemplate.setInterceptors(Collections.singletonList(apiKeyInterceptor));
        return restTemplate;
    }
}