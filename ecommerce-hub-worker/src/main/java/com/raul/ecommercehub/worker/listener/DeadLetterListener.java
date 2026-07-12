package com.raul.ecommercehub.worker.listener;

import com.raul.ecommercehub.shared.domain.BatchItem;
import com.raul.ecommercehub.shared.domain.Product;
import com.raul.ecommercehub.shared.domain.Tenant;
import com.raul.ecommercehub.shared.messaging.RabbitMQNames;
import com.raul.ecommercehub.shared.messaging.SyncMessage;
import com.raul.ecommercehub.shared.repository.BatchItemRepository;
import com.raul.ecommercehub.shared.repository.ProductRepository;
import com.raul.ecommercehub.shared.repository.TenantRepository;
import com.raul.ecommercehub.worker.alert.DiscordAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeadLetterListener {

    private final BatchItemRepository batchItemRepository;
    private final ProductRepository productRepository;
    private final TenantRepository tenantRepository;
    private final DiscordAlertService discordAlertService;

    @RabbitListener(queues = RabbitMQNames.SYNC_DLQ)
    @Transactional
    public void handle(SyncMessage message) {
        log.warn("Message dead-lettered for batchItem={}, marking as DEAD_LETTER", message.batchItemId());

        batchItemRepository.findById(message.batchItemId()).ifPresent(item -> {
            item.markDeadLetter("Failed after exhausting all retry attempts");
            batchItemRepository.save(item);

            String productName = productRepository.findById(message.productId())
                    .map(Product::getName)
                    .orElse("Unknown product");

            String tenantName = tenantRepository.findById(message.tenantId())
                    .map(Tenant::getName)
                    .orElse("Unknown tenant");

            boolean blockedByCircuitBreaker = item.getAttemptCount() == 0;

            discordAlertService.sendDeadLetterAlert(
                    productName, tenantName, item.getId().toString(),
                    item.getAttemptCount(), blockedByCircuitBreaker);
        });
    }
}