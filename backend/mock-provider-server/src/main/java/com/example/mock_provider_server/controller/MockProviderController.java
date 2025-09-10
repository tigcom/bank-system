package com.example.mock_provider_server.controller;

import com.example.mock_provider_server.database.MockDatabase;
import com.example.mock_provider_server.dto.request.CheckBillRequest;
import com.example.mock_provider_server.dto.request.ProviderPaymentRequest;
import com.example.mock_provider_server.dto.response.ApiResponse;
import com.example.mock_provider_server.dto.response.BillDetailsResponse;
import com.example.mock_provider_server.dto.response.ProviderPaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/mock-api")
public class MockProviderController {

    private static final List<String> ELECTRICITY_PROVIDERS = List.of("EVN_HCM", "EVN_HN", "EVN_DN");
    private static final List<String> TELEPHONE_PROVIDERS = List.of("MOBI", "VIETTEL", "VINA", "VNM");

    private final MockDatabase mockDatabase;

    @GetMapping("/providers")
    public ResponseEntity<Map<String, List<MockDatabase.ProviderDto>>> getGroupedProviders() {
        return ResponseEntity.ok(mockDatabase.findAllProvidersGroupedByType());
    }
    @PostMapping("/electricity/check")
    public ApiResponse<BillDetailsResponse> checkElectricityBill(@RequestBody CheckBillRequest request) {
        return mockDatabase.findBillByCustomerAndProvider(request.getCustomerCode(), request.getProvider())
                .map(bill -> ApiResponse.<BillDetailsResponse>builder()
                        .code(200)
                        .message("Lấy thông tin thành công.")
                        .result(bill)
                        .build())
                .orElse(ApiResponse.<BillDetailsResponse>builder()
                        .code(404)
                        .message("Không tìm thấy hóa đơn tiền điện cho mã khách hàng này.")
                        .result(null)
                        .build());
    }
    @PostMapping("/electricity/pay")
    public ApiResponse<ProviderPaymentResponse> payElectricityBill(@RequestBody ProviderPaymentRequest request) {
        return performPayBill(request, ELECTRICITY_PROVIDERS);
    }


    @PostMapping("/telephone/query")
    public ApiResponse<BillDetailsResponse> checkTelephoneBill(@RequestBody CheckBillRequest request) {
        return mockDatabase.findBillByCustomerAndProvider(request.getCustomerCode(), request.getProvider())
                .map(bill -> ApiResponse.<BillDetailsResponse>builder()
                        .code(200)
                        .message("Lấy thông tin thành công.")
                        .result(bill).build())
                .orElse(ApiResponse.<BillDetailsResponse>builder()
                        .code(404)
                        .message("Không tìm thấy hóa đơn trả sau cho số điện thoại này.")
                        .result(null)
                        .build());
    }

    // API thanh toán hóa đơn điện thoại
    @PostMapping("/telephone/pay")
    public ApiResponse<ProviderPaymentResponse> payTelephoneBill(@RequestBody ProviderPaymentRequest request) {
        return performPayBill(request, TELEPHONE_PROVIDERS);
    }

    private ApiResponse<ProviderPaymentResponse> performPayBill(ProviderPaymentRequest request, List<String> allowedProviders) {
        return mockDatabase.findBillById(request.getBillId())
                .filter(bill -> allowedProviders.contains(bill.getProvider()))
                .map(billToPay -> {
                    if (!"UNPAID".equals(billToPay.getStatus())) {
                        return ApiResponse.<ProviderPaymentResponse>builder()
                                .code(409)
                                .message("Hóa đơn này đã được thanh toán.")
                                .result(null)
                                .build();
                    }
                    // Cập nhật trạng thái
                    billToPay.setStatus("PAID");
                    mockDatabase.save(billToPay);
                    System.out.println("MockDatabase: Đã cập nhật trạng thái hóa đơn " + billToPay.getBillId() + " thành PAID.");

                    // Trả về thành công
                    return ApiResponse.<ProviderPaymentResponse>builder()
                            .code(200)
                            .message("Nhà cung cấp đã ghi nhận thanh toán thành công.")
                            .result(ProviderPaymentResponse.builder()
                                    .status("SUCCESSFUL")
                                    .providerTransactionId(billToPay.getProvider() + "_PAY_STATEFUL_" + System.currentTimeMillis())
                                    .build())
                            .build();
                })
                .orElse(ApiResponse.<ProviderPaymentResponse>builder()
                        .code(404)
                        .message("Mã hóa đơn không tồn tại hoặc không hợp lệ.")
                        .result(null).build());
    }
}
