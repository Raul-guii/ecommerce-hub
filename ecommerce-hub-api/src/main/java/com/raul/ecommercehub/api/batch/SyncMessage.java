package com.raul.ecommercehub.api.batch;

import java.math.BigDecimal;
import java.util.UUID;

public record SyncMessage(
        UUID batchItemId,
        UUID productId,
        UUID tenantId,
        BigDecimal newPrice,
        Integer newStock
) {}