package com.memory.context.engine.infrastructure.api.error;

import com.memory.context.engine.domain.memory.exception.MemoryNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1️⃣ Validation errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("Validation failed");

        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.builder()
                        .code("VALIDATION_ERROR")
                        .message(message)
                        .timestamp(Instant.now())
                        .path(request.getRequestURI())
                        .build());
    }

    // 2️⃣ Domain exception
    @ExceptionHandler(MemoryNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleMemoryNotFound(
            MemoryNotFoundException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.builder()
                        .code("MEMORY_NOT_FOUND")
                        .message(ex.getMessage())
                        .timestamp(Instant.now())
                        .path(request.getRequestURI())
                        .build());
    }

    // 3️⃣ Catch-all fallback
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.builder()
                        .code("INTERNAL_ERROR")
                        .message("Something went wrong")
                        .timestamp(Instant.now())
                        .path(request.getRequestURI())
                        .build());
    }
}
