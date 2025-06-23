package com.example.transaction_service.service;

import com.example.common_service.services.CommonService;
import com.example.common_service.services.account.AccountQueryService;
import com.example.common_service.services.customer.CustomerQueryService;
import com.example.common_service.services.customer.CustomerService;
import com.example.transaction_service.dto.request.BillCheckRequest;
import com.example.transaction_service.dto.response.BillDetailsResponse;
import com.example.transaction_service.exception.AppException;
import com.example.transaction_service.exception.ErrorCode;
import com.example.transaction_service.gateways.impl.ElectricityGatewayImpl;
import com.example.transaction_service.gateways.impl.TelephoneGatewayImpl;
import com.example.transaction_service.mapper.TransactionMapper;
import com.example.transaction_service.repository.TransactionRepository;
import com.example.transaction_service.service.impl.TransactionServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
@SpringBootTest(classes = {
        TransactionServiceImpl.class,
        ElectricityGatewayImpl.class,
        TelephoneGatewayImpl.class,
        BillPaymentServiceIntegrationTest.TestConfig.class
})
@WireMockTest(httpPort = 8089)
@TestPropertySource(properties = {
        "provider.api.electricity.url=http://localhost:8089/evn-api",
        "provider.api.telephone.url=http://localhost:8089/telecom-api"
})
public class BillPaymentServiceIntegrationTest {

    @Configuration
    static class TestConfig {
        @Bean
        RestTemplate restTemplate() {
            return new RestTemplateBuilder().build();
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionRepository transactionRepository;

    @MockBean
    private TransactionMapper transactionMapper;

    @MockBean
    private AccountQueryService accountQueryService;
    @MockBean
    private CustomerQueryService customerService;

    @MockBean
    private CommonService commonService;

    @MockBean
    private RedisTemplate<String,String> redisTemplate;

    @MockBean
    private StreamBridge streamBridge;

    @BeforeEach
    void setupStubs() throws JsonProcessingException {
        WireMock.resetAllRequests();

        // Kịch bản 1: Kiểm tra hóa đơn điện thành công
        BillDetailsResponse electricityBill = BillDetailsResponse.builder()
                .billId("EVN-BILL-001").customerName("TRUNG NGUYEN").amount(BigDecimal.valueOf(1200000)).provider("EVN HCMC").build();

        stubFor(post(urlEqualTo("/evn-api/check"))
                .withRequestBody(matchingJsonPath("$.customerCode", equalTo("EVN_SUCCESS")))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(electricityBill))));

        // Kịch bản 2: Không tìm thấy hóa đơn điện
        stubFor(post(urlEqualTo("/evn-api/check"))
                .withRequestBody(matchingJsonPath("$.customerCode", equalTo("EVN_NOT_FOUND")))
                .willReturn(aResponse().withStatus(404)));

        // Kịch bản 3: Kiểm tra hóa đơn điện thoại thành công
        BillDetailsResponse telephoneBill = BillDetailsResponse.builder()
                .billId("TEL-BILL-002").customerName("GIA HUY").amount(BigDecimal.valueOf(250000)).provider("Viettel").build();

        stubFor(post(urlEqualTo("/telecom-api/query"))
                .withRequestBody(matchingJsonPath("$.customerCode", equalTo("MOBI_SUCCESS")))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(telephoneBill))));

        // Kịch bản 4: Lỗi server từ nhà cung cấp điện thoại
        stubFor(post(urlEqualTo("/telecom-api/query"))
                .withRequestBody(matchingJsonPath("$.customerCode", equalTo("MOBI_SERVER_ERROR")))
                .willReturn(aResponse().withStatus(500)));
    }


    //    TH tìm thấy hóa đơn điện của khách hàng
    @Test
    void whenCheckElectricityBill_withValidCustomer_shouldReturnBillDetails() {
        BillCheckRequest request = new BillCheckRequest();
        request.setBillType("ELECTRICITY");
        request.setCustomerCode("EVN_SUCCESS");

        BillDetailsResponse response = transactionService.checkBill(request);

        assertThat(response).isNotNull();
        assertThat(response.getBillId()).isEqualTo("EVN-BILL-001");
        assertThat(response.getCustomerName()).isEqualTo("TRUNG NGUYEN");

        verify(1, postRequestedFor(urlEqualTo("/evn-api/check")));
        verify(0, postRequestedFor(urlEqualTo("/telecom-api/query")));
    }

//    TH tìm thấy hóa đơn điện thoại
    @Test
    void whenCheckTelephoneBill_withValidCustomer_shouldReturnBillDetails() {
        BillCheckRequest request = new BillCheckRequest();
        request.setBillType("TELEPHONE");
        request.setCustomerCode("MOBI_SUCCESS");

        BillDetailsResponse response = transactionService.checkBill(request);

        assertThat(response).isNotNull();
        assertThat(response.getProvider()).isEqualTo("Viettel");

        verify(1, postRequestedFor(urlEqualTo("/telecom-api/query")));
        verify(0, postRequestedFor(urlEqualTo("/evn-api/check")));
    }

    // TH tìm hóa đơn với mã khách hàng k tồn tại
    @Test
    void whenCheckElectricityBill_withInvalidCustomer_shouldThrowBillNotFoundException() {
        BillCheckRequest request = new BillCheckRequest();
        request.setBillType("ELECTRICITY");
        request.setCustomerCode("EVN_NOT_FOUND");

        AppException exception = Assertions.assertThrows(AppException.class, () -> {
            transactionService.checkBill(request);
        });

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BILL_NOT_FOUND);
        verify(1, postRequestedFor(urlEqualTo("/evn-api/check")));
    }

    //: Kiểm tra cách hệ thống xử lý khi server của nhà cung cấp điện thoại bị lỗi (lỗi 5xx).
    @Test
    void whenCheckTelephoneBill_withProviderError_shouldThrowProviderServerErrorException() {
        BillCheckRequest request = new BillCheckRequest();
        request.setBillType("TELEPHONE");
        request.setCustomerCode("MOBI_SERVER_ERROR");

        AppException exception = Assertions.assertThrows(AppException.class, () -> {
            transactionService.checkBill(request);
        });

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PROVIDER_SERVER_ERROR);
        verify(1, postRequestedFor(urlEqualTo("/telecom-api/query")));
    }

//    TH không hỗ trợ hóa đơn
    @Test
    void whenCheckBill_withUnsupportedType_shouldThrowException() {
        BillCheckRequest request = new BillCheckRequest();
        request.setBillType("INTERNET");
        request.setCustomerCode("FPT11223");

        Assertions.assertThrows(AppException.class, () -> {
            transactionService.checkBill(request);
        });
    }
}