package com.raul.ecommercehub.worker.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class MarketplaceClient {

    private final RestClient restClient;

    public MarketplaceClient() {
        this.restClient = RestClient.builder()
                .baseUrl("http://localhost:8082")
                .build();
    }

    public void sync() {
        restClient.post()
                .uri("/marketplace/sync")
                .retrieve()
                .toBodilessEntity();
    }
}