package com.example.account_service.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import java.io.IOException;

@Slf4j
public class ApiKeyFilter implements Filter {

    @Value("${app.api.key}")
    private String apiKey;

    public ApiKeyFilter(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if (servletRequest.getDispatcherType() != DispatcherType.REQUEST) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;

        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
        log.info("DispatcherType: " + httpRequest.getDispatcherType());
        log.info("Check api key");
        // Chỉ áp dụng filter cho /api/v1/create-initial-payment-account
        log.info("URL: " + httpRequest.getRequestURL());
        if (!httpRequest.getRequestURI().equals("/account/api/v1/create-initial-payment-account")) {
            log.info("change filter");
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        log.info("abc: ");
        // Lấy X-API-Key từ header
        String receivedApiKey = httpRequest.getHeader("X-API-Key");
        log.info("receivedApiKey: " + receivedApiKey);
        // Kiểm tra API Key
        if (apiKey.equals(receivedApiKey)) {
            log.info(" api key hợp lệ");
            filterChain.doFilter(servletRequest, servletResponse); // API Key hợp lệ
        } else {
            log.info("API Key does not match");
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"error\": \"Invalid API Key\"}");
        }
    }
}