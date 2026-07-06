package com.raul.ecommercehub.shared.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "users")
public class User extends TenantOwnedEntity {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    protected User() {
    }

    public User(UUID id, UUID tenantId, String email, String passwordHash, Role role, Status status) {
        super(tenantId);
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
    }

    public boolean isActive() {
        return status == Status.ACTIVE;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }

    public enum Role { ADMIN, OPERATOR }
    public enum Status { ACTIVE, DISABLED }
}