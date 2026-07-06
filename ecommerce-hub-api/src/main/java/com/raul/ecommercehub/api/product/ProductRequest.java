package com.raul.ecommercehub.api.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank(message = "SKU é obrigatório") String sku,
        @NotBlank(message = "Nome é obrigatório") String name,
        @NotNull(message = "Estoque é obrigatório") @PositiveOrZero(message = "Estoque não pode ser negativo") Integer stockQuantity,
        @NotNull(message = "Preço é obrigatório") @Positive(message = "Preço deve ser maior que zero") BigDecimal price
) {}