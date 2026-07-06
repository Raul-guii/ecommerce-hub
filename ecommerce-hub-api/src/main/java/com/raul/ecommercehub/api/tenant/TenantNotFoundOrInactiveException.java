package com.raul.ecommercehub.api.tenant;

import java.util.UUID;

public class TenantNotFoundOrInactiveException extends RuntimeException {

    public TenantNotFoundOrInactiveException(UUID tenantId) {
        super("Tenant not found or inactive: " + tenantId);
    }
}
