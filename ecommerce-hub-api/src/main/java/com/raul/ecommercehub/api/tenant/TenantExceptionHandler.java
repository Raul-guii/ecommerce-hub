package com.raul.ecommercehub.api.tenant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Deliberately generic response: never confirms or denies whether a
 * specific tenant id exists. Folds into the project's broader
 * GlobalExceptionHandler once step 4 introduces it.
 */
@RestControllerAdvice
public class TenantExceptionHandler {

    @ExceptionHandler(TenantNotFoundOrInactiveException.class)
    public ResponseEntity<Map<String, String>> handleTenantNotFoundOrInactive() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Access denied"));
    }
}
