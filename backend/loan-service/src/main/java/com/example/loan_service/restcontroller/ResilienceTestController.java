package com.example.loan_service.restcontroller;

import com.example.loan_service.dto.response.ApiResponseWrapper;
import com.example.loan_service.service.TestResilienceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/resilience-test")
@RequiredArgsConstructor
public class ResilienceTestController {
	private final TestResilienceService testService;

	@GetMapping("/unstable")
	public ResponseEntity<ApiResponseWrapper<String>> unstable(
			@RequestParam(defaultValue = "true") boolean fail,
			@RequestParam(defaultValue = "0") long delay
	) {
		ApiResponseWrapper<String> resp = new ApiResponseWrapper<>();
		try {
			String data = testService.unstableCall(fail, delay);
			resp.setStatus(HttpStatus.OK.value());
			resp.setMessage("UNSTABLE_OK");
			resp.setData(data);
			return ResponseEntity.ok(resp);
		} catch (Exception e) {
			resp.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
			resp.setMessage("UNSTABLE_ERROR: " + e.getMessage());
			return ResponseEntity.status(resp.getStatus()).body(resp);
		}
	}

	@GetMapping("/ratelimiter")
	public ResponseEntity<ApiResponseWrapper<String>> rateLimiter(
			@RequestParam(defaultValue = "ping") String note
	) {
		ApiResponseWrapper<String> resp = new ApiResponseWrapper<>();
		try {
			String data = testService.rateLimitedCall(note);
			resp.setStatus(HttpStatus.OK.value());
			resp.setMessage("RATE_OK");
			resp.setData(data);
			return ResponseEntity.ok(resp);
		} catch (Exception e) {
			resp.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
			resp.setMessage("RATE_ERROR: " + e.getMessage());
			return ResponseEntity.status(resp.getStatus()).body(resp);
		}
	}

	@GetMapping("/bulkhead")
	public ResponseEntity<ApiResponseWrapper<String>> bulkhead(
			@RequestParam(defaultValue = "10000") long delay
	) {
		ApiResponseWrapper<String> resp = new ApiResponseWrapper<>();
		try {
			String data = testService.slowCall(delay);
			resp.setStatus(HttpStatus.OK.value());
			resp.setMessage("BULKHEAD_OK");
			resp.setData(data);
			return ResponseEntity.ok(resp);
		} catch (Exception e) {
			resp.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
			resp.setMessage("BULKHEAD_ERROR: " + e.getMessage());
			return ResponseEntity.status(resp.getStatus()).body(resp);
		}
	}

	@GetMapping("/timelimiter")
	public CompletableFuture<ResponseEntity<ApiResponseWrapper<String>>> timeLimiter(
			@RequestParam(defaultValue = "35000") long delay
	) {
		return testService.timeLimitedCall(delay)
				.thenApply(data -> {
					ApiResponseWrapper<String> resp = new ApiResponseWrapper<>();
					resp.setStatus(HttpStatus.OK.value());
					resp.setMessage("TIME_OK");
					resp.setData(data);
					return ResponseEntity.ok(resp);
				})
				.exceptionally(ex -> {
					ApiResponseWrapper<String> resp = new ApiResponseWrapper<>();
					resp.setStatus(HttpStatus.GATEWAY_TIMEOUT.value());
					resp.setMessage("TIME_ERROR: " + ex.getMessage());
					return ResponseEntity.status(resp.getStatus()).body(resp);
				});
	}
} 