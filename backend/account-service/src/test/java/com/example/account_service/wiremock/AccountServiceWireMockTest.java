package com.example.account_service.wiremock;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
public class AccountServiceWireMockTest {
    @Autowired
    private RestTemplate restTemplate;
    private WireMockServer wireMockServer;
    @BeforeEach
    void setup() {
        wireMockServer = new WireMockServer(8081); // Default port 8089
        wireMockServer.start();
        WireMock.configureFor("localhost", 8081);
    }

    @AfterEach
    void teardown() {
        wireMockServer.stop();
    }
//    @Test
//    void testSuccessfulResponse() throws IOException {
//        String expectedResponse = "{ \"id\": 123, \"username\": \"testuser\", \"email\": \"test@example.com\" }";
//        stubFor(get(urlEqualTo("/api/users/123"))
//                .willReturn(aResponse()
//                        .withStatus(HttpStatus.OK.value())
//                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
//                        .withBody(expectedResponse)));
//
//
//        JsonNode user = restTemplate.getForObject("http://localhost:8089/api/users/123", JsonNode.class);
//
//        // Thực hiện các assertions trên kết quả
//        assertNotNull(user);
//        assertEquals(123, user.get("id").asInt());
//        assertEquals("testuser", user.get("username").asText());
//        assertEquals("test@example.com", user.get("email").asText());
//    }
//    @Test
//    void testGetUserDetails_ApiReturnsNotFound() {
//
//        stubFor(get(urlEqualTo("/api/users/456"))
//                .willReturn(aResponse()
//                        .withStatus(HttpStatus.NOT_FOUND.value())));
//
//        try {
//            restTemplate.getForObject("http://localhost:8089/api/users/456", JsonNode.class);
//            fail("Should have thrown an exception");
//        } catch (HttpClientErrorException ex) {
//            assertEquals(HttpStatus.NOT_FOUND,ex.getStatusCode());
//        }
//    }

}
