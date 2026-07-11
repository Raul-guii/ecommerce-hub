package com.raul.ecommercehub.shared.domain;

import com.raul.ecommercehub.shared.domain.enums.MarketplaceType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenant_integration_configs")
@Getter
@NoArgsConstructor
public class TenantIntegrationConfig extends TenantOwnedEntity {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MarketplaceType marketplace;

    @Column(name = "access_token_encrypted", nullable = false)
    private String accessTokenEncrypted;

    @Column(name = "refresh_token_encrypted")
    private String refreshTokenEncrypted;

    @Column(name = "token_expires_at", nullable = false)
    private LocalDateTime tokenExpiresAt;

    public TenantIntegrationConfig(UUID id, MarketplaceType marketplace,
                                   String accessTokenEncrypted,
                                   String refreshTokenEncrypted,
                                   LocalDateTime tokenExpiresAt) {
        this.id = id;
        this.marketplace = marketplace;
        this.accessTokenEncrypted = accessTokenEncrypted;
        this.refreshTokenEncrypted = refreshTokenEncrypted;
        this.tokenExpiresAt = tokenExpiresAt;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(tokenExpiresAt);
    }
}