package com.raul.ecommercehub.shared.messaging;

import java.math.BigDecimal;
import java.util.UUID;

public record SyncMessage(
        UUID batchItemId,
        UUID productId,
        UUID tenantId,
        BigDecimal newPrice,
        Integer newStock
) {}