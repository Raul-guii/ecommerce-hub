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

/**
 * The root entity — does NOT extend {@link TenantOwnedEntity}. A tenant
 * owns data, it isn't owned by one.
 */
@Entity
@Table(name = "tenants")
public class Tenant {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String cnpj;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Plan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Tenant() {
        // JPA
    }

    public Tenant(UUID id, String name, String cnpj, Plan plan, TenantStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.cnpj = cnpj;
        this.plan = plan;
        this.status = status;
        this.createdAt = createdAt;
    }

    public boolean isActive() {
        return status == TenantStatus.ACTIVE;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public TenantStatus getStatus() {
        return status;
    }

    public enum Plan {
        FREE, PRO, ENTERPRISE
    }

    public enum TenantStatus {
        ACTIVE, SUSPENDED
    }
}
