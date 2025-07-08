package com.example.transaction_service.gateways.impl;

import com.example.transaction_service.dto.request.ProviderPaymentRequest;
import com.example.transaction_service.dto.response.ApiResponse;
import com.example.transaction_service.dto.response.BillDetailsResponse;
import com.example.transaction_service.dto.response.ProviderPaymentResponse;
import com.example.transaction_service.exception.AppException;
import com.example.transaction_service.exception.ErrorCode;
import com.example.transaction_service.gateways.ProviderGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component("TELEPHONE")
@Slf4j
public class TelephoneGatewayImpl implements ProviderGateway {


    private final RestTemplate mockServerRestTemplate;
    private final String apiUrl;

    public TelephoneGatewayImpl(@Qualifier("mockServerRestTemplate") RestTemplate restTemplate
                                , @Value("${provider.api.telephone.url}") String apiUrl) {
        this.mockServerRestTemplate = restTemplate;
        this.apiUrl = apiUrl;
    }

    @Override
    public BillDetailsResponse checkBill(String customerCode, String provider) {
        String fullUrl = this.apiUrl + "/query";
        log.info("[TELEPHONE][checkBill] Gọi kiểm tra hóa đơn | customerCode: {} | provider: {} | endpoint: {}", customerCode, provider, fullUrl);
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("customerCode", customerCode);
        requestBody.put("provider", provider);
        HttpEntity<Object> entity = new HttpEntity<>(requestBody);
        try {
            ParameterizedTypeReference<ApiResponse<BillDetailsResponse>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<BillDetailsResponse>> responseEntity =
                    mockServerRestTemplate.exchange(fullUrl, HttpMethod.POST, entity, responseType);
            ApiResponse<BillDetailsResponse> apiResponse = responseEntity.getBody();
            if (apiResponse == null) {
                log.warn("[TELEPHONE][checkBill] Không có dữ liệu trả về từ provider | customerCode: {} | provider: {}", customerCode, provider);
                throw new AppException(ErrorCode.BILL_NOT_FOUND);
            }
            log.info("[TELEPHONE][checkBill] Kết quả trả về: {}", apiResponse);
            return apiResponse.getResult();
        } catch (HttpClientErrorException e) {
            log.error("[TELEPHONE][checkBill] Lỗi client khi gọi API: {}", e.getResponseBodyAsString());
            throw new AppException(ErrorCode.BILL_NOT_FOUND);
        } catch (RestClientException e) {
            log.error("[TELEPHONE][checkBill] Lỗi server/provider: {}", e.getMessage());
            throw new AppException(ErrorCode.PROVIDER_SERVER_ERROR);
        }
    }

    @Override
    public ProviderPaymentResponse payBill(ProviderPaymentRequest request) {
        String fullUrl = this.apiUrl + "/pay";
        log.info("[TELEPHONE][payBill] Gọi xác nhận thanh toán | endpoint: {} | request: {}", fullUrl, request);
        HttpEntity<ProviderPaymentRequest> entity = new HttpEntity<>(request);
        try {
            ParameterizedTypeReference<ApiResponse<ProviderPaymentResponse>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<ProviderPaymentResponse>> responseEntity =
                    mockServerRestTemplate.exchange(fullUrl, HttpMethod.POST, entity, responseType);
            ApiResponse<ProviderPaymentResponse> apiResponse = responseEntity.getBody();
            if (apiResponse == null || apiResponse.getCode() != 200 || apiResponse.getResult() == null) {
                log.warn("[TELEPHONE][payBill] Provider trả về lỗi | code: {} | message: {}", apiResponse != null ? apiResponse.getCode() : null, apiResponse != null ? apiResponse.getMessage() : null);
                if(apiResponse != null && apiResponse.getCode()==404) throw new AppException(ErrorCode.BILL_NOT_FOUND);
                else if (apiResponse != null && apiResponse.getCode()==409) {
                    throw new AppException(ErrorCode.BILL_PAID);
                }
                throw new AppException(ErrorCode.PROVIDER_PAYMENT_FAILED);
            }
            log.info("[TELEPHONE][payBill] Thanh toán thành công | providerTransactionId: {}", apiResponse.getResult().getProviderTransactionId());
            return apiResponse.getResult();
        } catch (HttpClientErrorException e) {
            log.error("[TELEPHONE][payBill] Lỗi client khi gọi API: {}", e.getResponseBodyAsString());
            throw new AppException(ErrorCode.PROVIDER_PAYMENT_FAILED);
        } catch (RestClientException e) {
            log.error("[TELEPHONE][payBill] Lỗi server/provider: {}", e.getMessage());
            throw new AppException(ErrorCode.PROVIDER_SERVER_ERROR);
        }
    }

    @Override
    public String getProviderType() {
        log.info("[TELEPHONE][getProviderType] Được gọi");
        return "TELEPHONE";
    }
}