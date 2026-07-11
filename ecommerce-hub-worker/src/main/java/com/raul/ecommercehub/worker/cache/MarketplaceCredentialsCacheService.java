package com.raul.ecommercehub.worker.cache;

import com.raul.ecommercehub.shared.domain.TenantIntegrationConfig;
import com.raul.ecommercehub.shared.domain.enums.MarketplaceType;
import com.raul.ecommercehub.shared.repository.TenantIntegrationConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketplaceCredentialsCacheService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(50);

    private final RedisTemplate<String, Object> redisTemplate;
    private final TenantIntegrationConfigRepository tenantIntegrationConfigRepository;

    public TenantIntegrationConfig getCredentials(UUID tenantId, MarketplaceType marketplace) {
        String cacheKey = buildKey(tenantId, marketplace);

        TenantIntegrationConfig cached = (TenantIntegrationConfig) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("Cache HIT for tenant={}, marketplace={}", tenantId, marketplace);
            return cached;
        }

        log.debug("Cache MISS for tenant={}, marketplace={} — fetching from MySQL", tenantId, marketplace);

        TenantIntegrationConfig config = tenantIntegrationConfigRepository
                .findByTenantIdAndMarketplace(tenantId, marketplace)
                .orElseThrow(() -> new IllegalStateException(
                        "No integration config found for tenant=" + tenantId + ", marketplace=" + marketplace));

        redisTemplate.opsForValue().set(cacheKey, config, CACHE_TTL);
        return config;
    }

    private String buildKey(UUID tenantId, MarketplaceType marketplace) {
        return "marketplace-token:%s:%s".formatted(tenantId, marketplace);
    }
}