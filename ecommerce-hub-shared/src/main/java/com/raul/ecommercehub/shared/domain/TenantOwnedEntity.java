package com.raul.ecommercehub.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.ParamDef;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/**
 * Base class for every entity that belongs to exactly one tenant.
 *
 * <p>The {@link Filter} below is NOT active by default — Hibernate applies
 * it to every query made through this superclass's subclasses only once
 * {@code TenantFilterInterceptor} explicitly enables it for the current
 * request/session, with a tenant id that was already validated server-side.
 * Never enable this filter with a tenant id taken directly from client input.
 */
@MappedSuperclass
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = String.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public abstract class TenantOwnedEntity {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    protected TenantOwnedEntity() {
        // JPA
    }

    protected TenantOwnedEntity(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getTenantId() {
        return tenantId;
    }
}
