package com.raul.ecommercehub.api.auth;

import java.util.UUID;

public record AuthenticatedPrincipal(UUID userId, UUID tenantId) {
}