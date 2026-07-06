package com.raul.ecommercehub.shared.repository;

import com.raul.ecommercehub.shared.domain.BatchItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BatchItemRepository extends JpaRepository<BatchItem, UUID> {
    List<BatchItem> findAllByBatchId(UUID batchId);
}