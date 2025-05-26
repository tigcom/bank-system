package com.example.account_service.exception;

import com.example.account_service.dto.response.ApiResponseWrapper;
import com.example.account_service.utils.MessageUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.nio.file.AccessDeniedException;
import java.util.Locale;
import java.util.NoSuchElementException;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {


    private final MessageSource messageSource;
    private final MessageUtils messageUtils;

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponseWrapper<?>> handleNotFoundException(NoHandlerFoundException ex) {
        ApiResponseWrapper<?> response = ApiResponseWrapper.error(HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<ApiResponseWrapper<?>> handleAllExceptions(Exception ex) {
//        ApiResponseWrapper<?> response = ApiResponseWrapper.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), messageUtils.getMessage("uncategorized.error"));
//        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
//    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponseWrapper<?>> handleAppException(AppException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        ApiResponseWrapper<?> response = ApiResponseWrapper.error(errorCode.getCode(),messageUtils.getMessage(errorCode.getMessage()) );
        return new ResponseEntity<>(response,HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseWrapper<?>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String messagekey = ex.getFieldError().getDefaultMessage();
       String messageLocale =messageSource.getMessage(messagekey, null, LocaleContextHolder.getLocale());
        ApiResponseWrapper<?> response = ApiResponseWrapper.error(HttpStatus.BAD_REQUEST.value(), messageLocale);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

//    @ExceptionHandler(AuthorizationDeniedException.class)
//    public ResponseEntity<ApiResponseWrapper<?>> handleAccessDeniedException(AuthorizationDeniedException ext) {
//        ApiResponseWrapper<?> response = ApiResponseWrapper.error(HttpStatus.FORBIDDEN.value(),messageUtils.getMessage("error.unauthorizated"));
//        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
//    }
}



