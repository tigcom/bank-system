package com.example.customer_service.configs;

import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class SecurityBypassProperties {

    public List<Pair<String, String>> getBypassEndpoints() {
        return Arrays.asList(
                Pair.of("/api/customers/register", "POST"),
                Pair.of("/api/customers/login", "POST"),
                Pair.of("/api/customers/kyc/verify", "POST"),

                // Swagger + actuator
                Pair.of("/swagger-ui.html", "GET"),
                Pair.of("/swagger-ui/**", "GET"),
                Pair.of("/v3/api-docs", "GET"),
                Pair.of("/v3/api-docs/**", "GET"),
                Pair.of("/swagger-resources/**", "GET"),
                Pair.of("/webjars/**", "GET"),
                Pair.of("/configuration/**", "GET"),
                Pair.of("/favicon.ico", "GET"),
                Pair.of("/actuator/circuitbreakers", "GET"),
                Pair.of("/actuator/health", "GET")
        );
    }
}

