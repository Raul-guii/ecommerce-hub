CREATE TABLE tenant_integration_configs (
    id CHAR(36) NOT NULL,
    tenant_id CHAR(36) NOT NULL,
    marketplace VARCHAR(20) NOT NULL,
    access_token_encrypted VARCHAR(1000) NOT NULL,
    refresh_token_encrypted VARCHAR(1000),
    token_expires_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_tenant_integration_configs_tenant_id (tenant_id),
    CONSTRAINT fk_tenant_integration_configs_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
);