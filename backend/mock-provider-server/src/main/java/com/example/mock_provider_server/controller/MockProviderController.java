package com.example.mock_provider_server.controller;

import com.example.mock_provider_server.dto.request.CheckBillRequest;
import com.example.mock_provider_server.dto.request.ProviderPaymentRequest;
import com.example.mock_provider_server.dto.response.ApiResponse;
import com.example.mock_provider_server.dto.response.BillDetailsResponse;
import com.example.mock_provider_server.dto.response.ProviderPaymentResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/mock-api")
public class MockProviderController {

    private static final Map<String, BillDetailsResponse> billDatabase = new ConcurrentHashMap<>();

    static {
        BillDetailsResponse electricityBill = BillDetailsResponse.builder()
                .billId("bill_stateful_01")
                .customerCode("EVN_STATEFUL")
                .customerName("NGUYEN THI TRANG THAI")
                .amount(BigDecimal.valueOf(75000))
                .provider("EVN_HCMC")
                .status("UNPAID")
                .build();
        BillDetailsResponse telephoneBill = BillDetailsResponse.builder()
                .billId("bill_tel_stateful_02")
                .customerCode("MOBI_STATEFUL")
                .customerName("TRAN VAN VIEN THONG")
                .amount(BigDecimal.valueOf(35000))
                .provider("MobiFone")
                .status("UNPAID")
                .build();
        billDatabase.put("bill_stateful_01", electricityBill);
        billDatabase.put("bill_tel_stateful_02", telephoneBill);
    }

    @PostMapping("/electricity/check")
    public ApiResponse<BillDetailsResponse> checkElectricityBill(@RequestBody CheckBillRequest request){
        String customerCode = request.getCustomerCode();

        for (BillDetailsResponse bill : billDatabase.values()) {
            if ("EVN_HCMC".equals(bill.getProvider()) && bill.getCustomerCode().equals(customerCode)) {
                return ApiResponse.<BillDetailsResponse>builder()
                        .code(200)
                        .message("Lấy thông tin thành công.")
                        .result(bill)
                        .build();
            }
        }

        return ApiResponse.<BillDetailsResponse>builder()
                .code(404)
                .message("Không tìm thấy hóa đơn nào cho mã khách hàng này.")
                .result(null)
                .build();
    }

    @PostMapping("/electricity/pay")
    public ApiResponse<ProviderPaymentResponse> payElectricityBill(@RequestBody ProviderPaymentRequest request) {
        String billId = request.getBillId();

        System.out.println("MockProvider: Nhận được yêu cầu thanh toán cho hóa đơn " + billId);

        BillDetailsResponse billToPay = billDatabase.get(billId);
        if (billToPay == null|| !"EVN_HCMC".equals(billToPay.getProvider())) {
            return ApiResponse.<ProviderPaymentResponse>builder()
                    .code(404)
                    .message("Mã hóa đơn không tồn tại.")
                    .result(null)
                    .build();
        }

        // Kiểm tra trạng thái hiện tại của hóa đơn
        if (!"UNPAID".equals(billToPay.getStatus())) {
            return ApiResponse.<ProviderPaymentResponse>builder()
                    .code(409)
                    .message("Hóa đơn này đã được thanh toán hoặc đang ở trạng thái không hợp lệ.")
                    .result(null)
                    .build();
        }

        // THAY ĐỔI TRẠNG THÁI!
        billToPay.setStatus("PAID");
        billDatabase.put(billId, billToPay); // Cập nhật lại vào "database"

        System.out.println("MockProvider: Đã cập nhật trạng thái hóa đơn " + billId + " thành PAID.");

        // Trả về kết quả thành công
        return ApiResponse.<ProviderPaymentResponse>builder()
                .code(200)
                .message("Nhà cung cấp đã ghi nhận thanh toán thành công.")
                .result(ProviderPaymentResponse.builder()
                        .status("SUCCESSFUL")
                        .providerTransactionId("EVN_PAY_STATEFUL_" + System.currentTimeMillis())
                        .build())
                .build();
    }


    @PostMapping("/telephone/query")
    public ApiResponse<BillDetailsResponse> checkTelephoneBill(@RequestBody CheckBillRequest request) {
        String customerCode = request.getCustomerCode();

        // Tìm trong "database" các hóa đơn của khách hàng này
        for (BillDetailsResponse bill : billDatabase.values()) {
            if ("MobiFone".equals(bill.getProvider()) && bill.getCustomerCode().equals(customerCode)) {
                return ApiResponse.<BillDetailsResponse>builder()
                        .code(200)
                        .message("Lấy thông tin thành công.")
                        .result(bill)
                        .build();
            }
        }

        return ApiResponse.<BillDetailsResponse>builder()
                .code(404)
                .message("Không tìm thấy hóa đơn trả sau cho số điện thoại này.")
                .result(null)
                .build();
    }

    @PostMapping("/telephone/pay")
    public ApiResponse<ProviderPaymentResponse> payTelephoneBill(@RequestBody ProviderPaymentRequest request) {
        String billId = request.getBillId();
        System.out.println("MockProvider: Nhận được yêu cầu thanh toán cho hóa đơn điện thoại " + billId);

        // Lấy hóa đơn từ "database"
        BillDetailsResponse billToPay = billDatabase.get(billId);

        if (billToPay == null || !"MobiFone".equals(billToPay.getProvider())) {
            return ApiResponse.<ProviderPaymentResponse>builder()
                    .code(404)
                    .message("Mã hóa đơn điện thoại không tồn tại.")
                    .result(null)
                    .build();
        }

        if (!"UNPAID".equals(billToPay.getStatus())) {
            return ApiResponse.<ProviderPaymentResponse>builder()
                    .code(409)
                    .message("Hóa đơn điện thoại này đã được thanh toán trước đó.")
                    .result(null)
                    .build();
        }

        // THAY ĐỔI TRẠNG THÁI
        billToPay.setStatus("PAID");
        billDatabase.put(billId, billToPay);

        System.out.println("MockProvider: Đã cập nhật trạng thái hóa đơn " + billId + " thành PAID.");

        return ApiResponse.<ProviderPaymentResponse>builder()
                .code(200)
                .message("Nhà cung cấp MobiFone đã ghi nhận thanh toán thành công.")
                .result(ProviderPaymentResponse.builder()
                        .status("SUCCESSFUL")
                        .providerTransactionId("MOBI_PAY_STATEFUL_" + System.currentTimeMillis())
                        .build())
                .build();
    }
}
