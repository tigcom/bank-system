package com.example.account_service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class InterServiceRestTemplateConfig {

    @Value("${keycloak.auth-server-url:http://localhost:8180}")
    private String keycloakUrl;

    @Value("${keycloak.realm:myrealm}")
    private String realm;

    @Value("${keycloak.client-id:account-service}")
    private String clientId;

    @Value("${keycloak.client-secret:your-client-secret}")
    private String clientSecret;

    // Cache để lưu trữ access token
    private final Map<String, TokenCache> tokenCache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Bean(name = "interServiceRestTemplate")
    public RestTemplate interServiceRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        // Interceptor để tự động thêm access token
        ClientHttpRequestInterceptor interceptor = (request, body, execution) -> {
            try {
                String accessToken = getAccessToken();
                if (accessToken != null) {
                    request.getHeaders().add("Authorization", "Bearer " + accessToken);
                }
            } catch (Exception e) {
                log.error("Failed to get access token for inter-service communication", e);
            }
            return execution.execute(request, body);
        };

        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
        interceptors.add(interceptor);
        restTemplate.setInterceptors(interceptors);

        return restTemplate;
    }

    private String getAccessToken() {
        String cacheKey = clientId + ":" + realm;
        TokenCache cachedToken = tokenCache.get(cacheKey);

        // Kiểm tra xem token còn valid không (expire trước 30 giây để an toàn)
        if (cachedToken != null && cachedToken.isValid()) {
            return cachedToken.getAccessToken();
        }

        // Lấy token mới từ Keycloak
        return refreshAccessToken(cacheKey);
    }

    private String refreshAccessToken(String cacheKey) {
        try {
            RestTemplate tokenRestTemplate = new RestTemplate();
            String tokenUrl = keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("grant_type", "client_credentials");
            map.add("client_id", clientId);
            map.add("client_secret", clientSecret);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

            ResponseEntity<Map> response = tokenRestTemplate.postForEntity(tokenUrl, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> tokenResponse = response.getBody();
                String accessToken = (String) tokenResponse.get("access_token");
                Integer expiresIn = (Integer) tokenResponse.get("expires_in");

                // Cache token
                TokenCache tokenCacheEntry = new TokenCache(accessToken, System.currentTimeMillis() + (expiresIn * 1000L));
                tokenCache.put(cacheKey, tokenCacheEntry);

                log.info("Successfully obtained access token for inter-service communication");
                return accessToken;
            }
        } catch (Exception e) {
            log.error("Failed to obtain access token from Keycloak", e);
        }
        return null;
    }

    // Inner class để cache token
    private static class TokenCache {
        private final String accessToken;
        private final long expiryTime;

        public TokenCache(String accessToken, long expiryTime) {
            this.accessToken = accessToken;
            this.expiryTime = expiryTime;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public boolean isValid() {
            // Token còn valid nếu chưa expire và còn ít nhất 30 giây
            return System.currentTimeMillis() < (expiryTime - 30000);
        }
    }
} 