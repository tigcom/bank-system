package com.example.transaction_service.gateways.impl;

import com.example.transaction_service.dto.request.ProviderPaymentRequest;
import com.example.transaction_service.dto.response.ApiResponse;
import com.example.transaction_service.dto.response.BillDetailsResponse;
import com.example.transaction_service.dto.response.ProviderPaymentResponse;
import com.example.transaction_service.exception.AppException;
import com.example.transaction_service.exception.ErrorCode;
import com.example.transaction_service.gateways.ProviderGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

@Component("TELEPHONE")
@Slf4j
public class TelephoneGatewayImpl implements ProviderGateway {

    private final RestTemplate restTemplate;
    private final String apiUrl;

    public TelephoneGatewayImpl(RestTemplate restTemplate, @Value("${provider.api.telephone.url}") String apiUrl) {
        this.restTemplate = restTemplate;
        this.apiUrl = apiUrl;
    }

    @Override
    public BillDetailsResponse checkBill(String customerCode) {
        // Giả sử endpoint của nhà cung cấp điện thoại là /query
        String fullUrl = this.apiUrl + "/query";
        var requestBody = Collections.singletonMap("customerCode", customerCode);
        HttpEntity<Object> entity = new HttpEntity<>(requestBody);

        try {

            ParameterizedTypeReference<ApiResponse<BillDetailsResponse>> responseType =
                    new ParameterizedTypeReference<>() {};

            ResponseEntity<ApiResponse<BillDetailsResponse>> responseEntity =
                    restTemplate.exchange(fullUrl, HttpMethod.POST, entity, responseType);

            ApiResponse<BillDetailsResponse> apiResponse = responseEntity.getBody();


            if (apiResponse == null) {
                // Xử lý trường hợp body rỗng dù response là 200 OK
                throw new AppException(ErrorCode.BILL_NOT_FOUND);
            }

            return apiResponse.getResult();

        } catch (HttpClientErrorException e) {
            throw new AppException(ErrorCode.BILL_NOT_FOUND);
        } catch (RestClientException e) {
            // Bắt lỗi 5xx hoặc lỗi mạng và trả về lỗi server của nhà cung cấp
            throw new AppException(ErrorCode.PROVIDER_SERVER_ERROR);
        }
    }

    @Override
    public ProviderPaymentResponse payBill(ProviderPaymentRequest request) {
        String fullUrl = this.apiUrl + "/pay";
        log.info("Gọi đến nhà cung cấp điện thoại để xác nhận thanh toán: {}", fullUrl);
        HttpEntity<ProviderPaymentRequest> entity = new HttpEntity<>(request);

        try {
            // Định nghĩa kiểu dữ liệu trả về mong muốn: ApiResponse chứa ProviderPaymentResult
            ParameterizedTypeReference<ApiResponse<ProviderPaymentResponse>> responseType =
                    new ParameterizedTypeReference<>() {};

            // Dùng exchange để gọi API
            ResponseEntity<ApiResponse<ProviderPaymentResponse>> responseEntity =
                    restTemplate.exchange(fullUrl, HttpMethod.POST, entity, responseType);

            ApiResponse<ProviderPaymentResponse> apiResponse = responseEntity.getBody();

            if (apiResponse == null || apiResponse.getCode() != 200 || apiResponse.getResult() == null) {
                if(apiResponse.getCode()==404) throw new AppException(ErrorCode.BILL_NOT_FOUND);
                else if (apiResponse.getCode()==409) {
                    throw new AppException(ErrorCode.BILL_PAID);
                }
                throw new AppException(ErrorCode.PROVIDER_PAYMENT_FAILED);
            }

            // Nếu thành công, trả về đối tượng ProviderPaymentResult
            return apiResponse.getResult();

        } catch (HttpClientErrorException e) {
            log.error("Lỗi client khi gọi API payBill của nhà cung cấp điện: {}", e.getResponseBodyAsString());
            throw new AppException(ErrorCode.PROVIDER_PAYMENT_FAILED);

        } catch (RestClientException e) {
            // Bắt lỗi 5xx hoặc lỗi kết nối
            log.error("Lỗi server khi gọi API payBill của nhà cung cấp điện: {}", e.getMessage());
            throw new AppException(ErrorCode.PROVIDER_SERVER_ERROR);
        }
    }

    @Override
    public String getProviderType() {
        return "TELEPHONE";
    }
}