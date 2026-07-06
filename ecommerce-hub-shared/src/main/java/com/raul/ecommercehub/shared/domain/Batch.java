package com.raul.ecommercehub.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "batches")
public class Batch extends TenantOwnedEntity {

    public enum BatchStatus { PROCESSING, COMPLETED, FAILED }

    @JdbcTypeCode(SqlTypes.CHAR)
    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BatchStatus status;

    @Column(name = "total_items", nullable = false)
    private Integer totalItems;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Batch() {
        // JPA
    }

    public Batch(UUID id, UUID tenantId, Integer totalItems) {
        super(tenantId);
        this.id = id;
        this.status = BatchStatus.PROCESSING;
        this.totalItems = totalItems;
        this.createdAt = LocalDateTime.now();
    }

    public void markCompleted() {
        this.status = BatchStatus.COMPLETED;
    }

    public void markFailed() {
        this.status = BatchStatus.FAILED;
    }

    public UUID getId() { return id; }
    public BatchStatus getStatus() { return status; }
    public Integer getTotalItems() { return totalItems; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}