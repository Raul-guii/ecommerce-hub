CREATE TABLE users (
   id CHAR(36) NOT NULL,
   tenant_id CHAR(36) NOT NULL,
   email VARCHAR(255) NOT NULL,
   password_hash VARCHAR(255) NOT NULL,
   role VARCHAR(20) NOT NULL,
   status VARCHAR(20) NOT NULL,
   PRIMARY KEY (id),
   UNIQUE KEY uk_users_email (email),
   KEY idx_users_tenant_id (tenant_id),
   CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
);