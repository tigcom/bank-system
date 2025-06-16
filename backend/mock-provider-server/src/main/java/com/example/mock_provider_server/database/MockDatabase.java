package com.example.mock_provider_server.database;


import com.example.mock_provider_server.dto.response.BillDetailsResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class MockDatabase {
    public record ProviderDto(String name, String value) {
    }

    private static final Map<String, BillDetailsResponse> billDatabase = new ConcurrentHashMap<>();

    private static final Map<String, String> PROVIDER_CODE_TO_NAME_MAP = Map.ofEntries(
            Map.entry("EVN_HN", "EVN Hà Nội"),
            Map.entry("EVN_HCM", "EVN Hồ Chí Minh"),
            Map.entry("EVN_DN", "EVN Đà Nẵng"),
            Map.entry("EVN_CT", "EVN Cần Thơ"),
            Map.entry("VIETTEL", "Viettel"),
            Map.entry("MOBI", "Mobifone"),
            Map.entry("VINA", "Vinaphone"),
            Map.entry("VNM", "Vietnamobile")
    );

    private static final Map<String, String> PROVIDER_CODE_TO_SERVICE_TYPE_MAP = Map.ofEntries(
            Map.entry("EVN_HN", "electricity"),
            Map.entry("EVN_HCM", "electricity"),
            Map.entry("EVN_DN", "electricity"),
            Map.entry("EVN_CT", "electricity"),
            Map.entry("VIETTEL", "phone"),
            Map.entry("MOBI", "phone"),
            Map.entry("VINA", "phone"),
            Map.entry("VNM", "phone")
    );

    @PostConstruct
    public void init() {
        BillDetailsResponse electricityBill1 = BillDetailsResponse.builder().billId("bill_stateful_01").customerCode("EVNHCM_001").customerName("NGUYEN THI TRANG").amount(BigDecimal.valueOf(75000)).provider("EVN_HCM").status("UNPAID").build();
        BillDetailsResponse electricityBill2 = BillDetailsResponse.builder().billId("bill_evnhn_02").customerCode("EVNHN_002").customerName("LE ANH QUAN").amount(BigDecimal.valueOf(45000)).provider("EVN_HN").status("UNPAID").build();
        BillDetailsResponse electricityBill3 = BillDetailsResponse.builder().billId("bill_evndn_03").customerCode("EVNDN_003").customerName("PHAM THI HOA").amount(BigDecimal.valueOf(20500)).provider("EVN_DN").status("PAID").build();
        BillDetailsResponse telephoneBill1 = BillDetailsResponse.builder().billId("bill_tel_stateful_02").customerCode("MOBI_001").customerName("TRAN VAN VIEN THONG").amount(BigDecimal.valueOf(35000)).provider("MOBI").status("UNPAID").build();
        BillDetailsResponse telephoneBill2 = BillDetailsResponse.builder().billId("bill_viettel_04").customerCode("VIETTEL_004").customerName("HOANG VAN HUNG").amount(BigDecimal.valueOf(18000)).provider("VIETTEL").status("UNPAID").build();
        BillDetailsResponse telephoneBill3 = BillDetailsResponse.builder().billId("bill_vina_05").customerCode("VINA_005").customerName("VO NGOC LAN").amount(BigDecimal.valueOf(99000)).provider("VINA").status("UNPAID").build();
        BillDetailsResponse telephoneBill4 = BillDetailsResponse.builder().billId("bill_vnm_06").customerCode("VNM_006").customerName("DANG ANH TUAN").amount(BigDecimal.valueOf(50000)).provider("VNM").status("PAID").build();

        billDatabase.put(electricityBill1.getBillId(), electricityBill1);
        billDatabase.put(electricityBill2.getBillId(), electricityBill2);
        billDatabase.put(electricityBill3.getBillId(), electricityBill3);
        billDatabase.put(telephoneBill1.getBillId(), telephoneBill1);
        billDatabase.put(telephoneBill2.getBillId(), telephoneBill2);
        billDatabase.put(telephoneBill3.getBillId(), telephoneBill3);
        billDatabase.put(telephoneBill4.getBillId(), telephoneBill4);
    }
    public Optional<BillDetailsResponse> findBillByCustomerAndProvider(String customerCode, String provider) {
        return billDatabase.values().stream()
                .filter(bill -> provider.equals(bill.getProvider()) && bill.getCustomerCode().equals(customerCode))
                .findFirst();
    }

    public Optional<BillDetailsResponse> findBillById(String billId) {
        return Optional.ofNullable(billDatabase.get(billId));
    }

    public void save(BillDetailsResponse bill) {
        billDatabase.put(bill.getBillId(), bill);
    }

    public Map<String, List<ProviderDto>> findAllProvidersGroupedByType() {
        return billDatabase.values().stream()
                .map(BillDetailsResponse::getProvider)
                .distinct()
                .collect(Collectors.groupingBy(
                        providerCode -> PROVIDER_CODE_TO_SERVICE_TYPE_MAP.getOrDefault(providerCode, "unknown"),
                        Collectors.mapping(
                                providerCode -> new ProviderDto(
                                        PROVIDER_CODE_TO_NAME_MAP.getOrDefault(providerCode, providerCode),
                                        providerCode
                                ),
                                Collectors.toList()
                        )
                ));
    }
}

