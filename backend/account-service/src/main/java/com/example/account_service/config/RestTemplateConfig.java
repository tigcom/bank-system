package com.example.account_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Configuration(proxyBeanMethods = false)
public class RestTemplateConfig {

    @Bean(name = "restTemplateInternal")
    public RestTemplate restTemplateInternal() {
        RestTemplate restTemplate = new RestTemplate();

        // Tạo interceptor để thêm Authorization header
        ClientHttpRequestInterceptor interceptor = (request, body, execution) -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
                String token = jwtAuthenticationToken.getToken().getTokenValue();
                request.getHeaders().add("Authorization", "Bearer " + token);
            } else {
                // Xử lý trường hợp không có JWT token (ví dụ: bỏ qua hoặc thêm logic khác)
                System.out.println("No JWT token found in authentication context. Skipping Authorization header.");
            }
            return execution.execute(request, body);
        };

        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
        interceptors.add(interceptor);
        restTemplate.setInterceptors(interceptors);
        return restTemplate;
    }

}