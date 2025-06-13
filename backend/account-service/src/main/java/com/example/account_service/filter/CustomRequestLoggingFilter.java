package com.example.account_service.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class CustomRequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(CustomRequestLoggingFilter.class);

    private static final String AUTHORIZATION_HEADER = "authorization";
    private static final String REPLACEMENT_STRING = "[PROTECTED]";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        System.out.println(">>> Filter triggered for: " + request.getRequestURI());
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);

        // Đọc trước tham số để kích hoạt caching (cho form-urlencoded)
        wrappedRequest.getParameterMap();

        // Tiếp tục filter chain
        filterChain.doFilter(wrappedRequest, response);

        // Ghi log sau khi xử lý để chắc chắn body được đọc
        logRequest(wrappedRequest);
    }

    private void logRequest(ContentCachingRequestWrapper request) {
        if (!logger.isDebugEnabled()) {
            return; // Không log nếu không ở chế độ DEBUG
        }

        StringBuilder sb = new StringBuilder();
        sb.append("REQUEST DATA: ");
        sb.append(request.getMethod()).append(" ");
        sb.append(request.getRequestURI());

        if (request.getQueryString() != null) {
            sb.append("?").append(request.getQueryString());
        }

        sb.append(", client=").append(request.getRemoteAddr());
        sb.append(", user=").append(request.getRemoteUser() != null ? request.getRemoteUser() : "anonymous");

        sb.append(", headers=[");
        Map<String, String> headerMap = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            if (AUTHORIZATION_HEADER.equalsIgnoreCase(headerName)) {
                headerMap.put(headerName, REPLACEMENT_STRING);
            } else {
                headerMap.put(headerName, headerValue);
            }
        }
        sb.append(headerMap.entrySet().stream()
                .map(entry -> entry.getKey() + ":\"" + entry.getValue() + "\"")
                .collect(Collectors.joining(", ")));
        sb.append("]");

        if (request.getContentAsByteArray().length > 0) {
            try {
                String payload = new String(request.getContentAsByteArray(), request.getCharacterEncoding());
                sb.append(", payload=").append(payload);
            } catch (Exception e) {
                logger.warn("Could not read request payload", e);
            }
        }

        logger.debug(sb.toString());
    }
}
