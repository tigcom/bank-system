package com.example.loan_service.restcontroller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.time.Duration;

@RestController
public class S3FileController {

    @Autowired
    private S3Presigner s3Presigner;


    private final String bucketName = "bucket-microapp";
    private final String region = "ap-southeast-1";
    @GetMapping("/generate-presigned-url")
    public String generatePresignedUrl(@RequestParam String key) {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(r -> r
                .signatureDuration(Duration.ofMinutes(15))
                .putObjectRequest(objectRequest)
        );
        return presignedRequest.url().toString();
    }

    @PostMapping("/update-file-path")
    public void updateFilePath(@RequestBody ) {

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
}