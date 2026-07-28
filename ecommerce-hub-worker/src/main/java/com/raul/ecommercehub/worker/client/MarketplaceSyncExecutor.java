package com.raul.ecommercehub.worker.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarketplaceSyncExecutor {

    private final MarketplaceClient marketplaceClient;

    @CircuitBreaker(name = "marketplaceSync")
    public void sync() {
        marketplaceClient.sync();
    }
}