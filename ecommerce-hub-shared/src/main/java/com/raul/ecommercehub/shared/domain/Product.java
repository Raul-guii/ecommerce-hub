package com.raul.ecommercehub.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;


@Entity
@Table(name = "products")
public class Product extends TenantOwnedEntity {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Id
    private UUID id;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Product() {
        // JPA
    }

    public Product(UUID id, UUID tenantId, String sku, String name,
                   Integer stockQuantity, BigDecimal price, LocalDateTime updatedAt) {
        super(tenantId);
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.stockQuantity = stockQuantity;
        this.price = price;
        this.updatedAt = updatedAt;
    }

    public void updateDetails(String sku, String name, Integer stockQuantity, BigDecimal price) {
        this.sku = sku;
        this.name = name;
        this.stockQuantity = stockQuantity;
        this.price = price;
        this.updatedAt = LocalDateTime.now();
    }

    public void applyStockAndPrice(Integer newStock, BigDecimal newPrice) {
        // Usado pelo fluxo de Batch (etapa 4/5) — atualiza só o que veio preenchido,
        // já que newStock/newPrice são opcionais em cada BatchItem.
        if (newStock != null) {
            this.stockQuantity = newStock;
        }
        if (newPrice != null) {
            this.price = newPrice;
        }
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}