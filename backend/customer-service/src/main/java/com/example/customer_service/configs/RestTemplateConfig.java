package com.example.customer_service.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Configuration
public class RestTemplateConfig {

    @Value("${corebanking.api-key}")
    private String apiKey;

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        // Tạo interceptor để thêm X-API-Key header
        ClientHttpRequestInterceptor interceptor = (request, body, execution) -> {
            if (apiKey != null && !apiKey.isEmpty()) {
                request.getHeaders().add("X-API-Key", apiKey);
            }
            return execution.execute(request, body);
        };
        restTemplate.setInterceptors(Collections.singletonList(interceptor));
        return restTemplate;
    }
}