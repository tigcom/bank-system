package com.example.account_service.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "keycloak.auth-server-url=http://localhost:8180",
    "keycloak.realm=myrealm",
    "keycloak.client-id=account-service",
    "keycloak.client-secret=test-secret"
})
public class InterServiceRestTemplateConfigTest {

    @Autowired(required = false)
    private RestTemplate interServiceRestTemplate;

    @Test
    public void contextLoads() {
        // Kiểm tra rằng bean có thể được tạo mà không gặp lỗi
        assertNotNull(interServiceRestTemplate, "Inter-service RestTemplate should be configured");
    }

    @Test
    public void testRestTemplateHasInterceptors() {
        // Kiểm tra rằng RestTemplate có interceptors
        assertNotNull(interServiceRestTemplate.getInterceptors(), "RestTemplate should have interceptors");
        assertFalse(interServiceRestTemplate.getInterceptors().isEmpty(), "RestTemplate should have at least one interceptor");
    }
} 