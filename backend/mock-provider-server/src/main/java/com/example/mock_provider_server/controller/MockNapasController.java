package com.example.mock_provider_server.controller;

import com.example.mock_provider_server.dto.request.NapasInquiryRequest;
import com.example.mock_provider_server.dto.request.NapasTransferRequest;
import com.example.mock_provider_server.dto.response.NapasInquiryResponse;
import com.example.mock_provider_server.dto.response.ApiResponse;
import com.example.mock_provider_server.dto.response.NapasTransferResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/mock-napas")
public class MockNapasController {

    @PostMapping("/inquiry")
    public ApiResponse<NapasInquiryResponse> inquiryAccount(@RequestBody NapasInquiryRequest request) throws InterruptedException {
        String accountNumber = request.getAccountNumber();
        String bankCode = request.getBankCode();
        System.out.println("[MOCK NAPAS] Nhan yeu cau truy van cho so tai khoan: " + accountNumber);

        Thread.sleep(ThreadLocalRandom.current().nextInt(300, 800));

        // --- Các kịch bản giả lập ---
        if (accountNumber == null || accountNumber.isBlank()) {
            return ApiResponse.<NapasInquiryResponse>builder()
                    .code(400)
                    .message("Số tài khoản không được trống")
                    .result(null)
                    .build();
        }

        // 1. Kịch bản thành công
        if (accountNumber.startsWith("111") && bankCode.equals("970436")) {
            return ApiResponse.<NapasInquiryResponse>builder()
                    .code(200)
                    .message("Thông tin tài khoản")
                    .result(NapasInquiryResponse.builder()
                            .customerName("LÊ VĂN A")
                            .accountStatus("ACTIVE")
                            .build())
                    .build();
        }
//       Account không hoạt động
        if (accountNumber.startsWith("121") && bankCode.equals("970436")) {
            return ApiResponse.<NapasInquiryResponse>builder()
                    .code(200)
                    .message("Thông tin tài khoản")
                    .result(NapasInquiryResponse.builder()
                            .customerName("LÊ VĂN B")
                            .accountStatus("CLOSED")
                            .build())
                    .build();
        }

        // 2. Kịch bản không tìm thấy tài khoản
        if (accountNumber.startsWith("222")&&bankCode.equals("970436")) {
            return ApiResponse.<NapasInquiryResponse>builder()
                    .code(404)
                    .message("Không tìm thấy tài khoản")
                    .result(null)
                    .build();
        }

        // 3. Kịch bản ngân hàng hưởng bị timeout
        if (accountNumber.startsWith("333")&& bankCode.equals("970436")) {
            Thread.sleep(5000); // Giả lập chờ 5 giây rồi timeout
            return ApiResponse.<NapasInquiryResponse>builder()
                    .code(404)
                    .message("Ngân hàng thụ hưởng đang có lỗi")
                    .result(null)
                    .build();
        }


        return ApiResponse.<NapasInquiryResponse>builder()
                .code(404)
                .message("Không tìm thấy tài khoản")
                .result(null)
                .build();
    }
//    Giao dịch
    @PostMapping("/transfer")
    public ApiResponse<NapasTransferResponse> executeTransfer(@RequestBody NapasTransferRequest request) throws InterruptedException {
        System.out.println("[MOCK NAPAS] Nhan yeu cau chuyen tien den so tai khoan: " + request.getToAccountNumber());
        // Giả lập độ trễ mạng
        Thread.sleep(ThreadLocalRandom.current().nextInt(500, 1500));

        // --- Các kịch bản giả lập ---

        // 1. Kịch bản lỗi kỹ thuật từ phía NAPAS/Ngân hàng hưởng
        if ("9999999999".equals(request.getToAccountNumber())) {
            return ApiResponse.<NapasTransferResponse>builder()
                    .code(500)
                    .message("Lỗi ở ngân hàng thụ hưởng")
                    .result(NapasTransferResponse.builder()
                            .status("FAILED")
                            .gatewayTransactionId(null)
                            .message("Lỗi ở ngân hàng thụ hưởng")
                            .build())
                    .build();

        }

        // 2. Kịch bản giao dịch bị từ chối
        if ("8888888888".equals(request.getToAccountNumber())) {
            return ApiResponse.<NapasTransferResponse>builder()
                    .code(500)
                    .message("Giao dịch bị từ chối")
                    .result(NapasTransferResponse.builder()
                            .status("FAILED")
                            .gatewayTransactionId(null)
                            .message("Giao dịch bị từ chối")
                            .build())
                    .build();

        }
        // 3. Mặc định: Giao dịch thành công
        return ApiResponse.<NapasTransferResponse>builder()
                .code(200)
                .message("Giao dịch thành công")
                .result(NapasTransferResponse.builder()
                        .status("SUCCESS")
                        .gatewayTransactionId("NAPAS_MOCK_1750277850123")
                        .message("Giao dịch thành công")
                        .build())
                .build();
    }
}
