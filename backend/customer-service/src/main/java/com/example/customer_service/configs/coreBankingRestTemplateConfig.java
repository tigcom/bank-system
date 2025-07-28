package com.example.customer_service.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Configuration(proxyBeanMethods = false)
public class coreBankingRestTemplateConfig {
    @Value("${core-banking.api.key}")
    private String apiKey;

    @Bean(name = "coreBankingRestTemplate")
    public RestTemplate coreBankingRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        // Interceptor thêm API Key vào header
        ClientHttpRequestInterceptor apiKeyInterceptor = (request, body, execution) -> {
            request.getHeaders().add("X-API-Key", apiKey); // tuỳ hệ thống có thể là "x-api-key", "Authorization", v.v.
            return execution.execute(request, body);
        };
        restTemplate.setInterceptors(Collections.singletonList(apiKeyInterceptor));
        return restTemplate;
    }
}