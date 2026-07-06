package com.raul.ecommercehub.worker.listener;

import com.raul.ecommercehub.shared.domain.BatchItem;
import com.raul.ecommercehub.shared.messaging.RabbitMQNames;
import com.raul.ecommercehub.shared.messaging.SyncMessage;
import com.raul.ecommercehub.shared.repository.BatchItemRepository;
import com.raul.ecommercehub.shared.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class SyncListener {

    private final BatchItemRepository batchItemRepository;
    private final ProductRepository productRepository;

    @RabbitListener(queues = RabbitMQNames.SYNC_QUEUE)
    @Transactional
    public void handle(SyncMessage message) {
        log.info("Received sync message for batchItem={}, product={}", message.batchItemId(), message.productId());

        simulateExternalMarketplaceCall(message);

        BatchItem batchItem = batchItemRepository.findById(message.batchItemId())
                .orElseThrow(() -> new IllegalStateException("BatchItem not found: " + message.batchItemId()));

        productRepository.findById(message.productId()).ifPresent(product -> {
            product.applyStockAndPrice(message.newStock(), message.newPrice());
            productRepository.save(product);
        });

        batchItem.markSuccess();
        batchItemRepository.save(batchItem);

        log.info("BatchItem {} marked as SUCCESS", batchItem.getId());
    }

    /**
     * TEMPORARY (step 5): simulates calling Mercado Livre/Amazon. WireMock replaces
     * this in step 6. For now, any price under 100 simulates a failed call, so
     * retry/backoff/DLQ behavior can be tested without a real HTTP dependency.
     */
    private void simulateExternalMarketplaceCall(SyncMessage message) {
        if (message.newPrice() != null && message.newPrice().compareTo(BigDecimal.valueOf(100)) < 0) {
            log.warn("Simulated marketplace failure for batchItem={}", message.batchItemId());
            throw new RuntimeException("Simulated marketplace API failure");
        }
    }
}