package com.raul.ecommercehub.worker.alert;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@Slf4j
public class DiscordAlertService {

    private final RestClient restClient;

    public DiscordAlertService(@Value("${discord.webhook-url}") String webhookUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(webhookUrl)
                .build();
    }

    public void sendDeadLetterAlert(String productName, String tenantName, String batchItemId,
                                    int attemptCount, boolean blockedByCircuitBreaker) {
        String reason = blockedByCircuitBreaker
                ? "the circuit breaker blocked the request (marketplace already unstable)"
                : (attemptCount == 1 ? "1 attempt" : "%d attempts".formatted(attemptCount));

        String message = """
                🚨 Alert: Failed to sync "%s" on Amazon — %s.
                Tenant: %s | BatchItem ID: %s
                Check the dashboard to resolve.
                """.formatted(productName, reason, tenantName, batchItemId);

        try {
            restClient.post()
                    .body(Map.of("content", message))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Discord alert sent for batchItem={}", batchItemId);
        } catch (Exception e) {
            log.error("Failed to send Discord alert for batchItem={}: {}", batchItemId, e.getMessage());
        }
    }
}