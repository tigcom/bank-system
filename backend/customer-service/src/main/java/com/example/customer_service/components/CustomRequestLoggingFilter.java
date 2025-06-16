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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CustomRequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger("ACCESS_LOG");

    // Danh sách query parameters nhạy cảm cần ẩn
    private static final List<String> SENSITIVE_PARAMS = Arrays.asList(
            "password", "token", "access_token", "refresh_token", "api_key",
            "client_secret", "otp", "verification_code", "verificationCode",
            "jwt", "bearer", "auth", "authorization", "key", "secret"
    );

    // Danh sách header nhạy cảm cần ẩn
    private static final List<String> SENSITIVE_HEADERS = Arrays.asList(
            "authorization", "bearer", "token", "api_key", "client_secret", "jwt"
    );

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

            // Tạo URL an toàn (ẩn query parameters nhạy cảm)
            String fullUrl = uri;
            if (query != null && !query.isEmpty()) {
                String sanitizedQuery = sanitizeQueryString(query);
                fullUrl = uri + "?" + sanitizedQuery;
            }

            String ip = getClientIpAddress(request);
            int status = response.getStatus();
            String clientInfo = getClientInfo(request);
            String headers = getHeaders(request);

            // Log với thông tin cần thiết nhưng an toàn
            StringBuilder logMessage = new StringBuilder();
            logMessage.append("IP: ").append(ip)
                    .append(", Method: ").append(method)
                    .append(", URL: ").append(fullUrl)
                    .append(", Status: ").append(status)
                    .append(", Time: ").append(durationMs).append(" ms");

            if (clientInfo != null && !clientInfo.isEmpty()) {
                logMessage.append(", ClientInfo: ").append(clientInfo);
            }
            if (headers != null && !headers.isEmpty()) {
                logMessage.append(", Headers: ").append(headers);
            }

            // Log theo level phù hợp với status code
            if (status >= 500) {
                logger.error(logMessage.toString());
            } else if (status >= 400) {
                logger.warn(logMessage.toString());
            } else {
                logger.info(logMessage.toString());
            }
        }
    }

    /**
     * Lấy thông tin client (RemoteUser, SessionId)
     */
    private String getClientInfo(HttpServletRequest request) {
        StringBuilder clientInfo = new StringBuilder();
        String remoteUser = request.getRemoteUser();
        if (remoteUser != null) {
            clientInfo.append("RemoteUser=").append(remoteUser);
        }
        String sessionId = request.getSession(false) != null ? request.getSession().getId() : null;
        if (sessionId != null) {
            if (clientInfo.length() > 0) {
                clientInfo.append(", ");
            }
            clientInfo.append("SessionId=").append(sessionId);
        }
        return clientInfo.toString();
    }

    /**
     * Lấy headers từ request, ẩn các header nhạy cảm
     */
    private String getHeaders(HttpServletRequest request) {
        return Collections.list(request.getHeaderNames()).stream()
                .map(name -> name + "=" + (isSensitiveHeader(name) ? "***HIDDEN***" : truncateString(request.getHeader(name), 100)))
                .collect(Collectors.joining(", "));
    }

    /**
     * Kiểm tra xem header có phải là thông tin nhạy cảm không
     */
    private boolean isSensitiveHeader(String headerName) {
        String lowerHeaderName = headerName.toLowerCase();
        return SENSITIVE_HEADERS.stream()
                .anyMatch(sensitive -> lowerHeaderName.contains(sensitive));
    }

    /**
     * Ẩn các query parameters nhạy cảm
     */
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

    /**
     * Kiểm tra xem parameter có phải là thông tin nhạy cảm không
     */
    private boolean isSensitiveParam(String paramName) {
        String lowerParamName = paramName.toLowerCase();
        return SENSITIVE_PARAMS.stream()
                .anyMatch(sensitive -> lowerParamName.contains(sensitive));
    }

    /**
     * Lấy địa chỉ IP thực của client (xử lý trường hợp có proxy)
     */
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

    /**
     * Cắt ngắn chuỗi để tránh log quá dài
     */
    private String truncateString(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...";
    }

    /**
     * Loại trừ một số endpoint không cần log
     */
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