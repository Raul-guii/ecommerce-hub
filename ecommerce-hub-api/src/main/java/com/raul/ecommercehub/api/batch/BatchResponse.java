package com.raul.ecommercehub.api.batch;

import com.raul.ecommercehub.shared.domain.Batch;

import java.util.UUID;

public record BatchResponse(UUID batchId, Batch.BatchStatus status) {}