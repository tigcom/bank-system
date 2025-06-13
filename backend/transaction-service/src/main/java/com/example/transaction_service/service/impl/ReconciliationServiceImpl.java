package com.example.transaction_service.service.impl;

import com.example.transaction_service.dto.TransactionDTO;
import com.example.transaction_service.entity.Transaction;
import com.example.transaction_service.mapper.TransactionMapper;
import com.example.transaction_service.repository.TransactionRepository;
import com.example.transaction_service.service.ReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReconciliationServiceImpl implements ReconciliationService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

    @Override
    public void getDailyPaymentTransaction() {
        LocalDate reconciliationDate = LocalDate.now().minusDays(1);
        log.info("Bắt đầu tác vụ đối soát cho ngày: {}", reconciliationDate);

        LocalDateTime startOfDay = reconciliationDate.atStartOfDay();
        LocalDateTime endOfDay = reconciliationDate.plusDays(1).atStartOfDay();
        List<Transaction> transactions = transactionRepository.getDailyPaymentTransaction(startOfDay,endOfDay);

        Map<String, List<Transaction>> txnsByProvider = transactions.stream()
                .collect(Collectors.groupingBy(Transaction::getBillProviderCode));

        for(Map.Entry<String,List<Transaction>> entry: txnsByProvider.entrySet()){
            String providerCode = entry.getKey();
            List<Transaction> providerTxns = entry.getValue();
            log.info("Đang xử lý {} giao dịch cho nhà cung cấp: {}", providerTxns.size(), providerCode);
            try {
                // Tạo ra file báo cáo (ví dụ: Excel hoặc CSV)
                 byte[] reportFile = generateCsvReport(providerCode, providerTxns);

                // 4b. Gửi file báo cáo này cho nhà cung cấp qua SFTP hoặc Email
                // deliveryService.sendReportViaSftp(providerCode, reportFile);
                System.out.println(providerTxns);
                System.out.println(new String(reportFile, StandardCharsets.UTF_8));
                log.info("Đã tạo và gửi báo cáo thành công cho {}", providerCode);
            } catch (Exception e) {
                log.error("Lỗi khi xử lý báo cáo cho nhà cung cấp {}: {}", providerCode, e.getMessage());
                // Cần có cơ chế cảnh báo cho admin về lỗi này (ví dụ: gửi email, tin nhắn)
            }
        }
    }

    public byte[] generateCsvReport(String providerCode, List<Transaction> transactions) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));

        writer.println("ReferenceCode,CustomerCode,BillId,Amount,Status,Timestamp");

        for (Transaction txn : transactions) {
            writer.printf("%s,%s,%s,%s,%s,%s%n",
                    txn.getReferenceCode(),
                    txn.getBillCustomerCode(),
                    txn.getBillId(),
                    txn.getAmount(),
                    txn.getStatus(),
                    txn.getTimestamp()
            );
        }

        writer.flush();
        return outputStream.toByteArray();
    }
}
