package com.raul.ecommercehub.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "batch_items")
public class BatchItem {

    public enum BatchItemStatus { PENDING, SUCCESS, FAILED, DEAD_LETTER }

    @JdbcTypeCode(SqlTypes.CHAR)
    @Id
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "batch_id", nullable = false)
    private UUID batchId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "new_price")
    private BigDecimal newPrice;

    @Column(name = "new_stock")
    private Integer newStock;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BatchItemStatus status;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    @Column(name = "last_error", length = 500)
    private String lastError;

    protected BatchItem() {
        // JPA
    }

    public BatchItem(UUID id, UUID batchId, UUID productId, BigDecimal newPrice, Integer newStock) {
        this.id = id;
        this.batchId = batchId;
        this.productId = productId;
        this.newPrice = newPrice;
        this.newStock = newStock;
        this.status = BatchItemStatus.PENDING;
        this.attemptCount = 0;
    }

    public void markSuccess() {
        this.status = BatchItemStatus.SUCCESS;
    }

    public void markFailed(String error) {
        this.status = BatchItemStatus.FAILED;
        this.lastError = error;
        this.attemptCount++;
    }

    public void markDeadLetter(String reason) {
        this.status = BatchItemStatus.DEAD_LETTER;
        this.lastError = reason;
    }

    public UUID getId() { return id; }
    public UUID getBatchId() { return batchId; }
    public UUID getProductId() { return productId; }
    public BigDecimal getNewPrice() { return newPrice; }
    public Integer getNewStock() { return newStock; }
    public BatchItemStatus getStatus() { return status; }
    public Integer getAttemptCount() { return attemptCount; }
    public String getLastError() { return lastError; }
}