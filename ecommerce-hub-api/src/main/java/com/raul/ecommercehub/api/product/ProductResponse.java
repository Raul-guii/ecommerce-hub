package com.raul.ecommercehub.api.product;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String sku,
        String name,
        Integer stockQuantity,
        BigDecimal price,
        LocalDateTime updatedAt
) {}