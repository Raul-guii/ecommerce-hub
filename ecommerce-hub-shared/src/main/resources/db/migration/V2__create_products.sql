CREATE TABLE products (
    id CHAR(36) NOT NULL,
    tenant_id CHAR(36) NOT NULL,
    sku VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    stock_quantity INT NOT NULL,
    price DECIMAL(12,2) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_products_tenant_id (tenant_id),
    CONSTRAINT fk_products_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
);
