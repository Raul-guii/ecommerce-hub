package com.raul.ecommercehub.worker.listener;

import com.raul.ecommercehub.shared.domain.BatchItem;
import com.raul.ecommercehub.shared.domain.Product;
import com.raul.ecommercehub.shared.domain.TenantIntegrationConfig;
import com.raul.ecommercehub.shared.domain.enums.MarketplaceType;
import com.raul.ecommercehub.shared.messaging.RabbitMQNames;
import com.raul.ecommercehub.shared.messaging.SyncMessage;
import com.raul.ecommercehub.shared.repository.BatchItemRepository;
import com.raul.ecommercehub.shared.repository.ProductRepository;
import com.raul.ecommercehub.worker.cache.MarketplaceCredentialsCacheService;
import com.raul.ecommercehub.worker.client.MarketplaceClient;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class SyncListener {

    private final BatchItemRepository batchItemRepository;
    private final ProductRepository productRepository;
    private final MarketplaceCredentialsCacheService credentialsCacheService;
    private final MarketplaceClient marketplaceClient;
    private final BatchItemFailureRecorder failureRecorder;

    @RabbitListener(queues = RabbitMQNames.SYNC_QUEUE)
    @Transactional
    @CircuitBreaker(name = "marketplaceSync")
    public void handle(SyncMessage message) {
        log.info("Received sync message for batchItem={}, product={}", message.batchItemId(), message.productId());

        TenantIntegrationConfig credentials = credentialsCacheService.getCredentials(
                message.tenantId(), MarketplaceType.AMAZON);
        log.info("Using credentials for tenant={}, marketplace={}, expiresAt={}",
                message.tenantId(), credentials.getMarketplace(), credentials.getTokenExpiresAt());

        BatchItem batchItem = batchItemRepository.findById(message.batchItemId())
                .orElseThrow(() -> new IllegalStateException("BatchItem not found: " + message.batchItemId()));

        try {
            marketplaceClient.sync();
        } catch (Exception e) {
            failureRecorder.recordFailure(batchItem, e.getMessage());
            log.warn("Sync attempt failed for batchItem={}, attemptCount={}", batchItem.getId(), batchItem.getAttemptCount());
            throw e;
        }

        productRepository.findById(message.productId()).ifPresent(product -> {
            product.applyStockAndPrice(message.newStock(), message.newPrice());
            productRepository.save(product);
        });

        batchItem.markSuccess();
        batchItemRepository.save(batchItem);

        log.info("BatchItem {} marked as SUCCESS", batchItem.getId());
    }
}