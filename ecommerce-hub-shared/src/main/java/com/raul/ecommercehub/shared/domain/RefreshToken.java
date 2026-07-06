package com.raul.ecommercehub.shared.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Id
    private UUID id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "replaced_by_token_id")
    private UUID replacedByTokenId;

    protected RefreshToken() {
    }

    public RefreshToken(UUID id, UUID userId, String tokenHash, LocalDateTime expiresAt) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.revoked = false;
    }

    public boolean isUsable() {
        return !revoked && expiresAt.isAfter(LocalDateTime.now());
    }

    public void markRevoked() {
        this.revoked = true;
    }

    public void markReplacedBy(UUID newTokenId) {
        this.revoked = true;
        this.replacedByTokenId = newTokenId;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public boolean isRevoked() { return revoked; }
}