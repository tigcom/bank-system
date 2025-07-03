package com.opening.banking.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opening.banking.request.InfoIncomeRequestDto;
import com.opening.banking.response.TransactionDto;
import com.opening.banking.service.OpeningBankingService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpeningBankingServiceImpl implements OpeningBankingService {

    private final RestTemplate restTemplate;

    @Value("${openingbanking.api.url}")
    private String apiUrl;

    @Value("${openingbanking.api.key}")
    private String apiKey;

    @Override
    @CircuitBreaker(name = "checkIncome", fallbackMethod = "fallbackCheckIncome")
    @Retry(name = "checkIncome")
    public List<TransactionDto> checkIncome(InfoIncomeRequestDto req) {
        log.info("Sending checkIncome request: {}", req);
        HttpHeaders headers = new HttpHeaders();
        headers.set(apiKey, "ApiKey abc123xyz");
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<InfoIncomeRequestDto> entity = new HttpEntity<>(req, headers);
        try {
            ResponseEntity<String> resp = restTemplate.postForEntity(
                    apiUrl + "/transactions",
                    entity,
                    String.class
            );
            log.info("Received raw response: status={}, body={}", resp.getStatusCode(), resp.getBody());
            String bodyStr = resp.getBody();
            if (bodyStr != null && bodyStr.contains("\"status\"")) {
                log.warn("Response contains error object (not transaction array): {}", bodyStr);
                return Collections.emptyList();
            }
            ObjectMapper mapper = new ObjectMapper();
            TransactionDto[] body = mapper.readValue(bodyStr, TransactionDto[].class);
            List<TransactionDto> result = Arrays.asList(body);
            log.info("Parsed {} transactions from response", result.size());
            return result;
        } catch (HttpClientErrorException e) {
            log.error("HTTP error from banking service: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Collections.emptyList();
            }
            throw e;
        } catch (Exception e) {
            log.error("Exception while parsing response: {}, full=", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    private List<TransactionDto> fallbackCheckIncome(InfoIncomeRequestDto req, Throwable ex) {
        log.error("Fallback checkIncome triggered for {}: {}", req, ex.getMessage(), ex);
        return List.of();
    }
}
