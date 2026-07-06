package com.raul.ecommercehub.api.tenant;

import java.util.UUID;

/**
 * Holds the current request's tenant id. Set once per request by
 * {@link TenantFilterInterceptor} and ALWAYS cleared in a {@code finally}
 * block — without that, in a reused thread pool, a tenant id can leak into
 * the next, unrelated request handled by the same thread.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(UUID tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static UUID get() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
