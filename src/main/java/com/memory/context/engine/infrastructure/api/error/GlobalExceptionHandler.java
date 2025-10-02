package com.memory.context.engine.infrastructure.api.error;

import com.memory.context.engine.domain.common.exception.DomainException;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

        // -------------------- VALIDATION --------------------

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiErrorResponse> handleValidation(
                        MethodArgumentNotValidException ex,
                        HttpServletRequest request) {
                Map<String, String> errors = ex.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .collect(Collectors.toMap(
                                                FieldError::getField,
                                                FieldError::getDefaultMessage,
                                                (a, b) -> a));

                return ResponseEntity.badRequest().body(
                                ApiErrorResponse.builder()
                                                .code("VALIDATION_ERROR")
                                                .message("Validation failed")
                                                .details(errors)
                                                .build());
        }

        // -------------------- DOMAIN --------------------

        @ExceptionHandler(DomainException.class)
        public ResponseEntity<ApiErrorResponse> handleDomain(
                        DomainException ex,
                        HttpServletRequest request) {
                HttpStatus status = switch (ex.getCode()) {
                        case "RESOURCE_NOT_FOUND" -> HttpStatus.NOT_FOUND;
                        case "ACCESS_DENIED" -> HttpStatus.FORBIDDEN;
                        case "INVALID_MEMORY_STATE" -> HttpStatus.CONFLICT;
                        default -> HttpStatus.BAD_REQUEST;
                };

                return ResponseEntity.status(status).body(
                                ApiErrorResponse.builder()
                                                .code(ex.getCode())
                                                .message(ex.getMessage())
                                                .build());
        }

        // -------------------- FALLBACK --------------------

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiErrorResponse> handleUnexpected(
                        Exception ex,
                        HttpServletRequest request) {
                // Log the actual error for debugging
                org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class)
                                .error("Unexpected error on {}: {}", request.getRequestURI(), ex.getMessage(), ex);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                                ApiErrorResponse.builder()
                                                .code("INTERNAL_ERROR")
                                                .message("Unexpected error occurred")
                                                .build());
        }

        @ExceptionHandler(OptimisticLockException.class)
        public ResponseEntity<ApiErrorResponse> handleOptimisticLock(
                        OptimisticLockException ex,
                        HttpServletRequest request) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(ApiErrorResponse.builder()
                                                .code("CONCURRENT_MODIFICATION")
                                                .message("Memory was updated by another request. Retry.")
                                                .build());
        }

}
