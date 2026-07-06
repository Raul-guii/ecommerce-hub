package com.raul.ecommercehub.api.batch;

import com.raul.ecommercehub.api.auth.AuthenticatedPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/batches")
@RequiredArgsConstructor
public class BatchController {

    private final BatchService batchService;

    @PostMapping
    public ResponseEntity<BatchResponse> create(@AuthenticationPrincipal AuthenticatedPrincipal principal,
                                                @Valid @RequestBody BatchRequest request) {
        BatchResponse response = batchService.createBatch(principal.tenantId(), request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}