package com.example.loan_service.restcontroller;


import com.example.loan_service.entity.Loan;
import com.example.loan_service.service.LoanService;
import lombok.Data;
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
public class S3FileController {

    @Autowired
    private S3Presigner s3Presigner;

    @Autowired
    private LoanService loanService;

    @Value("${app.s3.bucket-name:bucket-microapp}")
    private String bucketName;

    @GetMapping("/generate-presigned-url")
    public String generatePresignedUrl(@RequestParam String key, @RequestParam(required = false) String contentType) {
        PutObjectRequest.Builder objectRequestBuilder = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key);
        if (contentType != null && !contentType.isBlank()) {
            objectRequestBuilder = objectRequestBuilder.contentType(contentType);
        }

        PutObjectRequest objectRequest = objectRequestBuilder.build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(r -> r
                .signatureDuration(Duration.ofMinutes(15))
                .putObjectRequest(objectRequest)
        );
        return presignedRequest.url().toString();
    }

    @PostMapping("/update-file-path")
    public ResponseEntity<Loan> updateFilePath(@RequestBody UpdateFilePathRequest request) {
        Loan loan = loanService.getLoanById(request.getLoanId());
        if (loan == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        loan.setPathFile(request.getFilePath());
        if (request.getDeclaredIncome() != null) {
            loan.setDeclaredIncome(request.getDeclaredIncome());
        }
        Loan saved = loanService.updateLoan(loan);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/display-file")
    public ResponseEntity<Void> displayFile(@RequestParam String filePath) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(filePath)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .getObjectRequest(getObjectRequest)
                .build();
        String presignedUrl = s3Presigner.presignGetObject(presignRequest).url().toString();
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                .header("Location", presignedUrl)
                .build();
    }

    @Data
    public static class UpdateFilePathRequest {
        private Long loanId;
        private String filePath;
        private BigDecimal declaredIncome;
    }
}