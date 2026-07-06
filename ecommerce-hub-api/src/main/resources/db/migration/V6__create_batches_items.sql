CREATE TABLE batch_items (
     id CHAR(36) NOT NULL,
     batch_id CHAR(36) NOT NULL,
     product_id CHAR(36) NOT NULL,
     new_price DECIMAL(12,2),
     new_stock INT,
     status VARCHAR(20) NOT NULL,
     attempt_count INT NOT NULL DEFAULT 0,
     last_error VARCHAR(500),
     PRIMARY KEY (id),
     KEY idx_batch_items_batch_id (batch_id),
     KEY idx_batch_items_product_id (product_id),
     CONSTRAINT fk_batch_items_batch FOREIGN KEY (batch_id) REFERENCES batches (id),
     CONSTRAINT fk_batch_items_product FOREIGN KEY (product_id) REFERENCES products (id)
);