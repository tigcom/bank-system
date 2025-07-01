package com.example.account_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;

import java.util.ArrayList;
import java.util.List;

@Configuration(proxyBeanMethods = false)
@Slf4j
public class RestTemplateConfig {

    @Value("${app.api.key}")
    private String apiKey;

    @Bean
    public RestTemplate restTemplate() {
        // Cấu hình timeout
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 5 giây
        factory.setReadTimeout(5000); // 5 giây

        RestTemplate restTemplate = new RestTemplate(factory);

        // Tạo interceptor để thêm X-API-Key header
        ClientHttpRequestInterceptor interceptor = (request, body, execution) -> {
            if (apiKey != null && !apiKey.isEmpty()) {
                request.getHeaders().add("X-API-Key", apiKey);
                log.debug("[RestTemplate] Đã thêm X-API-Key vào header cho yêu cầu");
            } else {
                log.warn("[RestTemplate] Không tìm thấy X-API-Key trong cấu hình. Yêu cầu có thể thất bại.");
            }
            return execution.execute(request, body);
        };

        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
        interceptors.add(interceptor);
        restTemplate.setInterceptors(interceptors);

        return restTemplate;
    }
}