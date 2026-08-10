CREATE TABLE batches (
    id CHAR(36) NOT NULL,
    tenant_id CHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL,
    total_items INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_batches_tenant_id (tenant_id),
    CONSTRAINT fk_batches_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
);