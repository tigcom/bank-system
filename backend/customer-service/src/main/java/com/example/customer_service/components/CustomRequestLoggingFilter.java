package com.example.customer_service.components;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
public class CustomRequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger("ACCESS_LOG");

    // parameters
    private static final List<String> SENSITIVE_PARAMS = Arrays.asList(
            "password", "token", "access_token", "refresh_token", "api_key",
            "client_secret", "otp", "verification_code", "verificationCode",
            "jwt", "bearer", "auth", "authorization", "key", "secret"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        Instant start = Instant.now();
        
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
            Instant end = Instant.now();
            long durationMs = Duration.between(start, end).toMillis();

            String method = request.getMethod();
            String uri = request.getRequestURI();
            String query = request.getQueryString();

            // Tạo URL an toàn (ẩn query parameters nhạy cảm)
            String fullUrl = uri;
            if (query != null && !query.isEmpty()) {
                String sanitizedQuery = sanitizeQueryString(query);
                fullUrl = uri + "?" + sanitizedQuery;
            }

            String ip = getClientIpAddress(request);
            int status = response.getStatus();

            // Log với thông tin cần thiết
            String logMessage = String.format("ACCESS_LOG - RequestId: %s, IP: %s, Method: %s, URL: %s, Status: %d, Duration: %d ms",
                    requestId, ip, method, fullUrl, status, durationMs);

            // Log theo level phù hợp với status code
            if (status >= 500) {
                logger.error(logMessage);
            } else if (status >= 400) {
                logger.warn(logMessage);
            } else {
                logger.info(logMessage);
            }
        }
    }

    // Ẩn các query parameters nhạy cảm
    private String sanitizeQueryString(String queryString) {
        if (queryString == null || queryString.isEmpty()) {
            return queryString;
        }

        StringBuilder sanitized = new StringBuilder();
        String[] params = queryString.split("&");

        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                sanitized.append("&");
            }

            String[] keyValue = params[i].split("=", 2);
            String key = keyValue[0].toLowerCase().trim();

            sanitized.append(keyValue[0]); // Giữ nguyên case gốc của key
            if (keyValue.length > 1) {
                sanitized.append("=");
                // Kiểm tra nếu là parameter nhạy cảm
                if (isSensitiveParam(key)) {
                    sanitized.append("***HIDDEN***");
                } else {
                    sanitized.append(keyValue[1]);
                }
            }
        }

        return sanitized.toString();
    }

    // Kiểm tra xem parameter có phải là thông tin nhạy cảm không
    private boolean isSensitiveParam(String paramName) {
        String lowerParamName = paramName.toLowerCase();
        return SENSITIVE_PARAMS.stream()
                .anyMatch(sensitive -> lowerParamName.contains(sensitive));
    }

    // Lấy địa chỉ IP thực của client (xử lý trường hợp có proxy)
    private String getClientIpAddress(HttpServletRequest request) {
        // Kiểm tra X-Forwarded-For header (load balancer/proxy)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            // Lấy IP đầu tiên trong danh sách (IP gốc của client)
            return xForwardedFor.split(",")[0].trim();
        }

        // Kiểm tra X-Real-IP header
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        // Kiểm tra X-Forwarded header
        String xForwarded = request.getHeader("X-Forwarded");
        if (xForwarded != null && !xForwarded.isEmpty() && !"unknown".equalsIgnoreCase(xForwarded)) {
            return xForwarded;
        }

        // Kiểm tra Forwarded header (RFC 7239)
        String forwarded = request.getHeader("Forwarded");
        if (forwarded != null && !forwarded.isEmpty()) {
            // Parse Forwarded header: for=192.0.2.60;proto=http;by=203.0.113.43
            String[] parts = forwarded.split(";");
            for (String part : parts) {
                if (part.trim().startsWith("for=")) {
                    String forValue = part.trim().substring(4);
                    // Loại bỏ quotes và port nếu có
                    forValue = forValue.replaceAll("\"", "").split(":")[0];
                    if (!forValue.isEmpty() && !"unknown".equalsIgnoreCase(forValue)) {
                        return forValue;
                    }
                }
            }
        }

        // Fallback về remote address mặc định
        return request.getRemoteAddr();
    }

    // Loại trừ một số endpoint không cần log
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        // Không log health check và actuator endpoints
        return path.contains("/actuator/") ||
                path.equals("/health") ||
                path.equals("/api/health") ||
                path.contains("/favicon.ico");
    }
}