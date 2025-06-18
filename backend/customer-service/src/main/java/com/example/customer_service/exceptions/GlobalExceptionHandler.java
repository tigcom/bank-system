package com.example.customer_service.exceptions;

import com.example.customer_service.responses.ApiResponseWrapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    private String getLocalizedMessage(String key) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(key, null, locale);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseWrapper<Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        // Lấy danh sách lỗi và chỉ lấy message (không lấy tên field)
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getDefaultMessage()) // chỉ lấy nội dung
                .collect(Collectors.joining("; "));

        return ResponseEntity.badRequest().body(
                new ApiResponseWrapper<>(HttpStatus.BAD_REQUEST.value(), errorMessage, null)
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponseWrapper<Object>> handleIllegalArgument(IllegalArgumentException ex) {
        String rawMessage = ex.getLocalizedMessage();
        String cleanedMessage = Arrays.stream(rawMessage.split(";"))
                .map(String::trim)
                .map(msg -> msg.contains(":") ? msg.substring(msg.indexOf(":") + 1).trim() : msg)
                .collect(Collectors.joining("; "));

        return ResponseEntity.badRequest().body(
                new ApiResponseWrapper<>(HttpStatus.BAD_REQUEST.value(), cleanedMessage, null)
        );
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponseWrapper<Object>> handleEntityNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ApiResponseWrapper<>(HttpStatus.NOT_FOUND.value(), ex.getLocalizedMessage(), null)
        );
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponseWrapper<Object>> handleBusinessException(BusinessException ex) {
        return ResponseEntity.badRequest().body(
                new ApiResponseWrapper<>(HttpStatus.BAD_REQUEST.value(), ex.getMessage(), null)
        );
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponseWrapper<Object>> handleRuntimeException(RuntimeException ex) {
        // Tránh trả message nhạy cảm
        String message = getLocalizedMessage("error.internal");
        return ResponseEntity.internalServerError().body(
                new ApiResponseWrapper<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), message, null)
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseWrapper<Object>> handleGeneralException(Exception ex) {
        String message = getLocalizedMessage("error.internal");
        return ResponseEntity.internalServerError().body(
                new ApiResponseWrapper<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), message, null)
        );
    }
}