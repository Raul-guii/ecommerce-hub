package com.raul.ecommercehub.api.exception;

import com.raul.ecommercehub.api.auth.InvalidCredentialsException;
import com.raul.ecommercehub.api.auth.InvalidJwtException;
import com.raul.ecommercehub.api.auth.RefreshTokenReuseDetectedException;
import com.raul.ecommercehub.api.tenant.TenantNotFoundOrInactiveException;
import com.raul.ecommercehub.api.batch.ProductSkuNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // --- Auth ---

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return error(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }

    @ExceptionHandler(RefreshTokenReuseDetectedException.class)
    public ResponseEntity<Map<String, String>> handleRefreshTokenReuse(RefreshTokenReuseDetectedException ex) {
        return error(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }

    @ExceptionHandler(InvalidJwtException.class)
    public ResponseEntity<Map<String, String>> handleInvalidJwt(InvalidJwtException ex) {
        return error(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
    }

    // --- Tenant ---

    @ExceptionHandler(TenantNotFoundOrInactiveException.class)
    public ResponseEntity<Map<String, String>> handleTenantNotFound(TenantNotFoundOrInactiveException ex) {
        // 401 em vez de 403/404 propositalmente: evita confirmar
        // se um tenant_id existe ou não pra quem está sondando a API.
        log.warn(ex.getMessage());
        return error(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }

    // --- Bean Validation (DTOs com @Valid) ---

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "Validation failed");
        body.put("fields", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // --- Fallback: nunca vaza stacktrace ---

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    }

    @ExceptionHandler(ProductSkuNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleProductSkuNotFound(ProductSkuNotFoundException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}