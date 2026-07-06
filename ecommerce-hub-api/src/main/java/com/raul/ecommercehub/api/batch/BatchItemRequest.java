package com.raul.ecommercehub.api.batch;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record BatchItemRequest(
        @NotBlank(message = "SKU é obrigatório") String sku,
        @Positive(message = "Preço deve ser maior que zero") BigDecimal price,
        @PositiveOrZero(message = "Estoque não pode ser negativo") Integer stock
) {}