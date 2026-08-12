package com.raul.ecommercehub.worker.cache;

import com.raul.ecommercehub.shared.domain.TenantIntegrationConfig;
import com.raul.ecommercehub.shared.domain.enums.MarketplaceType;
import com.raul.ecommercehub.shared.repository.TenantIntegrationConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Teste de unidade puro (sem @SpringBootTest/Testcontainers) — a classe só
 * depende de duas interfaces mockáveis (RedisTemplate, repository), então
 * não há necessidade de subir contexto Spring nem banco real.
 */
@ExtendWith(MockitoExtension.class)
class MarketplaceCredentialsCacheServiceTest {

    @Mock
    RedisTemplate<String, Object> redisTemplate;

    @Mock
    ValueOperations<String, Object> valueOperations;

    @Mock
    TenantIntegrationConfigRepository tenantIntegrationConfigRepository;

    MarketplaceCredentialsCacheService cacheService;

    UUID tenantId;
    MarketplaceType marketplace;
    String expectedKey;

    @BeforeEach
    void setUp() {
        cacheService = new MarketplaceCredentialsCacheService(redisTemplate, tenantIntegrationConfigRepository);
        tenantId = UUID.randomUUID();
        marketplace = MarketplaceType.AMAZON;
        expectedKey = "marketplace-token:%s:%s".formatted(tenantId, marketplace);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void getCredentials_cacheHit_retornaDoRedisSemConsultarMySQL() {
        TenantIntegrationConfig cached = new TenantIntegrationConfig(
                UUID.randomUUID(), marketplace, "cached-token", null,
                LocalDateTime.now().plusHours(1));
        when(valueOperations.get(expectedKey)).thenReturn(cached);

        TenantIntegrationConfig result = cacheService.getCredentials(tenantId, marketplace);

        assertThat(result).isSameAs(cached);
        verifyNoInteractions(tenantIntegrationConfigRepository);
        // Cache HIT nunca deve reescrever o valor — TTL não é renovado nessa
        // implementação (comportamento atual documentado pelo teste).
        verify(valueOperations, never()).set(anyString(), any(), any(Duration.class));
    }

    @Test
    void getCredentials_cacheMiss_buscaNoMySQLEPopulaORedisComTtlDe50Minutos() {
        TenantIntegrationConfig fromDb = new TenantIntegrationConfig(
                UUID.randomUUID(), marketplace, "db-token", null,
                LocalDateTime.now().plusHours(1));
        when(valueOperations.get(expectedKey)).thenReturn(null);
        when(tenantIntegrationConfigRepository.findByTenantIdAndMarketplace(tenantId, marketplace))
                .thenReturn(Optional.of(fromDb));

        TenantIntegrationConfig result = cacheService.getCredentials(tenantId, marketplace);

        assertThat(result).isSameAs(fromDb);

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).set(eq(expectedKey), eq(fromDb), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofMinutes(50));
    }

    @Test
    void getCredentials_cacheMissESemConfigNoMySQL_lancaIllegalStateExceptionSemPopularCache() {
        when(valueOperations.get(expectedKey)).thenReturn(null);
        when(tenantIntegrationConfigRepository.findByTenantIdAndMarketplace(tenantId, marketplace))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> cacheService.getCredentials(tenantId, marketplace))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(tenantId.toString())
                .hasMessageContaining(marketplace.toString());

        // Nunca escreve no cache um valor que nem existe.
        verify(valueOperations, never()).set(anyString(), any(), any(Duration.class));
    }
}