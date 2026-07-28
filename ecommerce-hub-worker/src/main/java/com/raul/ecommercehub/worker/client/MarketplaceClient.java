package com.raul.ecommercehub.worker.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class MarketplaceClient {

    private final RestClient restClient;

    public MarketplaceClient(@Value("${marketplace.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public void sync() {
        restClient.post()
                .uri("/marketplace/sync")
                .retrieve()
                .toBodilessEntity();
    }
}