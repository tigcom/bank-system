package com.example.loan_service.exception;

import com.example.loan_service.dto.response.ErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Autowired
    private MessageSource messageSource;

    // Helper để log info cho mọi handler
    private void logInfoException(String handlerName, Exception ex, WebRequest request) {
        log.info("GlobalExceptionHandler [{}] path={}, message={}", handlerName, request.getDescription(false), ex.getMessage(), ex);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex, WebRequest request) {
        logInfoException("DataIntegrityViolationException", ex, request);
        Locale locale = request.getLocale();
        String errorMessage = messageSource.getMessage("user.alreadyExists", new Object[]{ex.getMessage()}, locale);
        return new ResponseEntity<>(new ErrorResponse(HttpStatus.CONFLICT.value(), errorMessage), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadableException(HttpMessageNotReadableException ex, WebRequest request) {
        logInfoException("HttpMessageNotReadableException", ex, request);
        Locale locale = request.getLocale();
        String errorMessage = messageSource.getMessage("error.invalidInput", new Object[]{ex.getMessage()}, locale);
        return new ResponseEntity<>(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), errorMessage), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        logInfoException("ConstraintViolationException", ex, request);
        Locale locale = request.getLocale();
        String errorMessage = messageSource.getMessage("error.invalidInput", new Object[]{ex.getMessage()}, locale);
        return new ResponseEntity<>(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), errorMessage), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNoSuchElement(NoSuchElementException ex, WebRequest request) {
        logInfoException("NoSuchElementException", ex, request);
        Locale locale = request.getLocale();
        String errorMessage = messageSource.getMessage("user.notfound", new Object[]{ex.getMessage()}, locale);
        return new ResponseEntity<>(new ErrorResponse(HttpStatus.NOT_FOUND.value(), errorMessage), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(EmptyResultDataAccessException.class)
    public ResponseEntity<ErrorResponse> handleEmptyResult(EmptyResultDataAccessException ex, WebRequest request) {
        logInfoException("EmptyResultDataAccessException", ex, request);
        Locale locale = request.getLocale();
        String errorMessage = messageSource.getMessage("user.notfound", new Object[]{ex.getMessage()}, locale);
        return new ResponseEntity<>(new ErrorResponse(HttpStatus.NOT_FOUND.value(), errorMessage), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ResponseEntity<ErrorResponse> handleInvalidDataAccessApiUsageException(InvalidDataAccessApiUsageException ex, WebRequest request) {
        logInfoException("InvalidDataAccessApiUsageException", ex, request);
        Locale locale = request.getLocale();
        String errorMessage = messageSource.getMessage("error.invalidInput", new Object[]{ex.getMessage()}, locale);
        return new ResponseEntity<>(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), errorMessage), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException ex, WebRequest request) {
        logInfoException("EntityNotFoundException", ex, request);
        Locale locale = request.getLocale();
        String errorMessage = messageSource.getMessage("user.notfound", new Object[]{ex.getMessage()}, locale);
        return new ResponseEntity<>(new ErrorResponse(HttpStatus.NOT_FOUND.value(), errorMessage), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex, WebRequest request) {
        logInfoException("IllegalStateException", ex, request);
        Locale locale = request.getLocale();
        String errorMessage = messageSource.getMessage("error.invalidState", new Object[]{ex.getMessage()}, locale);
        return new ResponseEntity<>(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), errorMessage), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        logInfoException("IllegalArgumentException", ex, request);
        Locale locale = request.getLocale();
        String errorMessage = messageSource.getMessage("error.invalidArgument", new Object[]{ex.getMessage()}, locale);
        return new ResponseEntity<>(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), errorMessage), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ErrorResponse> handleResourceAccessException(ResourceAccessException ex, WebRequest request) {
        logInfoException("ResourceAccessException", ex, request);
        Locale locale = request.getLocale();
        String errorMessage = messageSource.getMessage("error.serviceUnavailable", new Object[]{ex.getMessage()}, locale);
        return new ResponseEntity<>(new ErrorResponse(HttpStatus.SERVICE_UNAVAILABLE.value(), errorMessage), HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<ErrorResponse> handleRestClientException(RestClientException ex, WebRequest request) {
        logInfoException("RestClientException", ex, request);
        Locale locale = request.getLocale();
        String errorMessage = messageSource.getMessage("error.externalServiceError", new Object[]{ex.getMessage()}, locale);
        return new ResponseEntity<>(new ErrorResponse(HttpStatus.BAD_GATEWAY.value(), errorMessage), HttpStatus.BAD_GATEWAY);
    }

    @ExceptionHandler(ArithmeticException.class)
    public ResponseEntity<ErrorResponse> handleArithmeticException(ArithmeticException ex, WebRequest request) {
        logInfoException("ArithmeticException", ex, request);
        Locale locale = request.getLocale();
        String errorMessage = messageSource.getMessage("error.calculationError", new Object[]{ex.getMessage()}, locale);
        return new ResponseEntity<>(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), errorMessage), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<ErrorResponse> handleNumberFormatException(NumberFormatException ex, WebRequest request) {
        logInfoException("NumberFormatException", ex, request);
        Locale locale = request.getLocale();
        String errorMessage = messageSource.getMessage("error.numberFormatError", new Object[]{ex.getMessage()}, locale);
        return new ResponseEntity<>(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), errorMessage), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(jakarta.persistence.PersistenceException.class)
    public ResponseEntity<ErrorResponse> handlePersistenceException(jakarta.persistence.PersistenceException ex, WebRequest request) {
        logInfoException("PersistenceException", ex, request);
        Locale locale = request.getLocale();
        String errorMessage = messageSource.getMessage("error.databaseError", new Object[]{ex.getMessage()}, locale);
        return new ResponseEntity<>(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), errorMessage), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, WebRequest request) {
        logInfoException("Exception", ex, request);
        Locale locale = request.getLocale();
        String errorMessage = messageSource.getMessage(
                "error.systemError",
                new Object[]{ex.getMessage()},
                "Unexpected system error occurred",
                locale
        );
        return new ResponseEntity<>(
                new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), errorMessage),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex, WebRequest request) {
        logInfoException("RuntimeException", ex, request);
        Locale locale = request.getLocale();
        String message = ex.getMessage();
        if (message != null) {
            if (message.contains("core banking")) {
                String errorMessage = messageSource.getMessage("coreBanking.error", new Object[]{message}, locale);
                return new ResponseEntity<>(new ErrorResponse(HttpStatus.BAD_GATEWAY.value(), errorMessage), HttpStatus.BAD_GATEWAY);
            } else if (message.contains("CIC")) {
                String errorMessage = messageSource.getMessage("cic.error", new Object[]{message}, locale);
                return new ResponseEntity<>(new ErrorResponse(HttpStatus.BAD_GATEWAY.value(), errorMessage), HttpStatus.BAD_GATEWAY);
            } else if (message.contains("loan")) {
                String errorMessage = messageSource.getMessage("loan.invalidStatus", new Object[]{message}, locale);
                return new ResponseEntity<>(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), errorMessage), HttpStatus.BAD_REQUEST);
            }
        }

        String errorMessage = messageSource.getMessage("error.general", new Object[]{message}, locale);
        return new ResponseEntity<>(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), errorMessage), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        logInfoException("MethodArgumentNotValidException", ex, request);
        Map<String, String> errors = new HashMap<>();
        Locale locale = request.getLocale();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = messageSource.getMessage("error.invalidInput", new Object[]{error.getDefaultMessage()}, locale);
            errors.put(fieldName, errorMessage);
        });
        log.info("[ValidationErrors] path={}, errors={}", request.getDescription(false), errors);
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }
}
