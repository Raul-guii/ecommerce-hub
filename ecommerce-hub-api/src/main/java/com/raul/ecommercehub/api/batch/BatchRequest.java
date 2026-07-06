package com.raul.ecommercehub.api.batch;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BatchRequest(
        @NotEmpty(message = "A lista de itens não pode ser vazia")
        List<@Valid BatchItemRequest> items
) {}