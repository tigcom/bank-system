package com.example.loan_service.restcontroller;

import com.example.loan_service.entity.Loan;
import com.example.loan_service.service.LoanService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.math.BigDecimal;
import java.time.Duration;

@RestController
@RequestMapping("/api/files")
@Slf4j
public class S3FileController {

    @Autowired
    private S3Presigner s3Presigner;

    @Autowired
    private LoanService loanService;

    @Value("${app.s3.bucket-name:bucket-microapp}")
    private String bucketName;

    @GetMapping("/generate-presigned-url")
    public String generatePresignedUrl(@RequestParam String key, @RequestParam(required = false) String contentType) {
        log.info("GENERATE_PRESIGNED_URL_START - bucketName: {}, key: {}, contentType: {}", bucketName, key, contentType);

        PutObjectRequest.Builder objectRequestBuilder = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key);
        if (contentType != null && !contentType.isBlank()) {
            objectRequestBuilder = objectRequestBuilder.contentType(contentType);
        }

        PutObjectRequest objectRequest = objectRequestBuilder.build();
        log.info("GENERATE_PRESIGNED_URL_OBJECT_REQUEST - {}", objectRequest);

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(r -> r
                .signatureDuration(Duration.ofMinutes(15))
                .putObjectRequest(objectRequest)
        );
        String url = presignedRequest.url().toString();
        log.info("GENERATE_PRESIGNED_URL_SUCCESS - url: {}", url);

        return url;
    }

    @PostMapping("/update-file-path")
    public ResponseEntity<Loan> updateFilePath(@RequestBody UpdateFilePathRequest request) {
        log.info("UPDATE_FILE_PATH_START - request: {}", request);

        Loan loan = loanService.getLoanById(request.getLoanId());
        if (loan == null) {
            log.info("UPDATE_FILE_PATH_NOT_FOUND - loanId: {}", request.getLoanId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        loan.setPathFile(request.getFilePath());
        if (request.getDeclaredIncome() != null) {
            loan.setDeclaredIncome(request.getDeclaredIncome());
        }

        Loan saved = loanService.updateLoan(loan);
        log.info("UPDATE_FILE_PATH_SUCCESS - loanId: {}, newPath: {}, declaredIncome: {}",
                saved.getLoanId(), saved.getPathFile(), saved.getDeclaredIncome());

        return ResponseEntity.ok(saved);
    }

    @GetMapping("/display-file")
    public ResponseEntity<String> displayFile(@RequestParam String filePath) {
        log.info("DISPLAY_FILE_START - bucketName: {}, filePath: {}", bucketName, filePath);

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(filePath)
                .build();
        log.info("DISPLAY_FILE_GET_OBJECT_REQUEST - {}", getObjectRequest);

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .getObjectRequest(getObjectRequest)
                .build();

        String presignedUrl = s3Presigner.presignGetObject(presignRequest).url().toString();
        log.info("DISPLAY_FILE_SUCCESS - presignedUrl: {}", presignedUrl);

        return ResponseEntity.ok(presignedUrl);
    }

    @Data
    public static class UpdateFilePathRequest {
        private Long loanId;
        private String filePath;
        private BigDecimal declaredIncome;

        @Override
        public String toString() {
            return "UpdateFilePathRequest{" +
                    "loanId=" + loanId +
                    ", filePath='" + filePath + '\'' +
                    ", declaredIncome=" + declaredIncome +
                    '}';
        }
    }
}
