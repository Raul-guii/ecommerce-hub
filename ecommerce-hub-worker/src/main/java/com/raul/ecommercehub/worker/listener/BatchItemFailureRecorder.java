package com.raul.ecommercehub.worker.listener;

import com.raul.ecommercehub.shared.domain.BatchItem;
import com.raul.ecommercehub.shared.repository.BatchItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class BatchItemFailureRecorder {

    private final BatchItemRepository batchItemRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(BatchItem batchItem, String errorMessage) {
        batchItem.markFailed(errorMessage);
        batchItemRepository.save(batchItem);
    }
}