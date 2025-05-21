//package com.example.customer_service.services;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//import org.springframework.web.reactive.function.client.WebClient;
//import reactor.core.publisher.Mono;
//
//@Service
//public class KycService {
//
//    private final WebClient webClient;
//
//    public KycService(WebClient.Builder webClientBuilder,
//                      @Value("${kyc.api.url}") String kycApiUrl,
//                      @Value("${kyc.api.key}") String kycApiKey) {
//        this.webClient = webClientBuilder
//                .baseUrl(kycApiUrl)
//                .defaultHeader("Authorization", "Bearer " + kycApiKey)
//                .build();
//    }
//
//    public Mono<String> verifyIdentity(String identityNumber, String fullName) {
//        String requestBody = String.format("""
//            {
//                "identity_number": "%s",
//                "full_name": "%s",
//                "country": "VN"
//            }
//            """, identityNumber, fullName);
//
//        return webClient.post()
//                .bodyValue(requestBody)
//                .retrieve()
//                .bodyToMono(String.class)
//                .onErrorReturn("{\"verified\": false, \"reason\": \"API error\"}");
//    }
//}
