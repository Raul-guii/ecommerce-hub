CREATE TABLE tenants (
    id CHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    cnpj VARCHAR(20) NOT NULL,
    plan VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenants_cnpj (cnpj)
);
