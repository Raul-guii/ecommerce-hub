package com.raul.ecommercehub.api.batch;

import com.raul.ecommercehub.api.config.RabbitMQConfig;
import com.raul.ecommercehub.shared.domain.Batch;
import com.raul.ecommercehub.shared.domain.BatchItem;
import com.raul.ecommercehub.shared.domain.Product;
import com.raul.ecommercehub.shared.repository.BatchItemRepository;
import com.raul.ecommercehub.shared.repository.BatchRepository;
import com.raul.ecommercehub.shared.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BatchService {

    private final BatchRepository batchRepository;
    private final BatchItemRepository batchItemRepository;
    private final ProductRepository productRepository;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public BatchResponse createBatch(UUID tenantId, BatchRequest request) {
        Batch batch = new Batch(UUID.randomUUID(), tenantId, request.items().size());
        batchRepository.save(batch);

        for (BatchItemRequest itemRequest : request.items()) {
            Product product = productRepository.findBySku(itemRequest.sku())
                    .orElseThrow(() -> new ProductSkuNotFoundException(itemRequest.sku()));

            BatchItem batchItem = new BatchItem(
                    UUID.randomUUID(),
                    batch.getId(),
                    product.getId(),
                    itemRequest.price(),
                    itemRequest.stock());
            batchItemRepository.save(batchItem);

            SyncMessage message = new SyncMessage(
                    batchItem.getId(), product.getId(), tenantId,
                    itemRequest.price(), itemRequest.stock());

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.SYNC_EXCHANGE,
                    RabbitMQConfig.SYNC_ROUTING_KEY,
                    message);
        }

        return new BatchResponse(batch.getId(), batch.getStatus());
    }

    @Transactional(readOnly = true)
    public List<BatchItem> findItemsByBatchId(UUID batchId) {
        return batchItemRepository.findAllByBatchId(batchId);
    }
}