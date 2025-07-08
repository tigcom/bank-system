package com.example.corebanking_service.Config;

<<<<<<< HEAD
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
=======
<<<<<<< HEAD
import jakarta.servlet.*;
=======
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
>>>>>>> 15e536bc976093c4e921fde702250bbcb4776b94
>>>>>>> main
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
<<<<<<< HEAD
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
=======
<<<<<<< HEAD
>>>>>>> main

import java.io.IOException;
@Slf4j

<<<<<<< HEAD
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    @Value("${api-key}")
    private String expectedApiKey;
=======
@Slf4j
public class ApiKeyFilter implements Filter {

    @Value("${core-banking.api.key}")
    private String apiKey;
>>>>>>> main

    public ApiKeyFilter(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
<<<<<<< HEAD
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String apiKey = request.getHeader("X-API-KEY");
        if (expectedApiKey.equals(apiKey)) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("Unauthorized access attempt to corebank from IP: {}", request.getRemoteAddr());
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("Invalid or missing API key");
        }
    }
}

=======
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
=======
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
@Slf4j

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    @Value("${api-key}")
    private String expectedApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String apiKey = request.getHeader("X-API-KEY");
        if (expectedApiKey.equals(apiKey)) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("Unauthorized access attempt to corebank from IP: {}", request.getRemoteAddr());
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("Invalid or missing API key");
        }
    }
}
>>>>>>> 15e536bc976093c4e921fde702250bbcb4776b94
>>>>>>> main
