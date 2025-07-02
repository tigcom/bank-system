package com.example.loan_service.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import static com.alibaba.com.caucho.hessian.io.HessianFactory.log;

@Configuration
public class RestTemplateConfig {
    @Value("${core-banking.api-key}")
    private String CORE_BANK_API_KEY;
    @Bean(name = "restTemplate")
    public RestTemplate restTemplate() {
        log.info("12312421421412");
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().add("X-API-KEY", CORE_BANK_API_KEY);
            return execution.execute(request, body);
        });
        return restTemplate;
    }
}

