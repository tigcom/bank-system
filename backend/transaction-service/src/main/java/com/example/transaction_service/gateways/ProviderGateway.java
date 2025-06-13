package com.example.transaction_service.gateways;

import com.example.transaction_service.dto.request.ProviderPaymentRequest;
import com.example.transaction_service.dto.response.BillDetailsResponse;
import com.example.transaction_service.dto.response.ProviderPaymentResponse;



public interface ProviderGateway {

//    Kiểm tra thông tin hóa đơn
    BillDetailsResponse checkBill(String customerCode);

    ProviderPaymentResponse payBill(ProviderPaymentRequest request);
    //   Trả về loại nhà cung cấp "ELECTRICITY" hoặc "TELEPHONE".
    String getProviderType();
}
