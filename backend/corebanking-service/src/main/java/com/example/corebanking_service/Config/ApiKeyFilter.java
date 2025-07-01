package com.example.corebanking_service.Config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;

@Slf4j
public class ApiKeyFilter implements Filter {

    @Value("${core-banking.api.key}")
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

        // Lấy X-API-Key từ header
        String receivedApiKey = httpRequest.getHeader("X-API-Key");
        log.info("receivedApiKey: " + receivedApiKey);
        log.info("this api key is: " + apiKey);
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