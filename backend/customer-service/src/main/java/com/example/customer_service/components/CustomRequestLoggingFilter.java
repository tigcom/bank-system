package com.example.customer_service.components;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

@Component
public class CustomRequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger("ACCESS_LOG");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Instant start = Instant.now();

        try {
            filterChain.doFilter(request, response);
        } finally {
            Instant end = Instant.now();
            long durationMs = Duration.between(start, end).toMillis();

            String method = request.getMethod();
            String uri = request.getRequestURI();
            String query = request.getQueryString();
            String fullUrl = query == null ? uri : uri + "?" + query;
            String ip = request.getRemoteAddr();
            int status = response.getStatus();

            logger.info("IP: {}, Method: {}, URL: {}, Status: {}, Time: {} ms",
                    ip, method, fullUrl, status, durationMs);
        }
    }
}

