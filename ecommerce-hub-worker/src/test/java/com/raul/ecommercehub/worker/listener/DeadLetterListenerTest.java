package com.raul.ecommercehub.worker.listener;

import com.raul.ecommercehub.shared.domain.BatchItem;
import com.raul.ecommercehub.shared.domain.Product;
import com.raul.ecommercehub.shared.domain.Tenant;
import com.raul.ecommercehub.shared.messaging.SyncMessage;
import com.raul.ecommercehub.shared.repository.BatchItemRepository;
import com.raul.ecommercehub.shared.repository.ProductRepository;
import com.raul.ecommercehub.shared.repository.TenantRepository;
import com.raul.ecommercehub.worker.alert.DiscordAlertService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * Cobre o DeadLetterListener chamando handle() diretamente (sem RabbitMQ
 * real, mesmo padrão do SyncListenerTest). DiscordAlertService é mockado —
 * o teste verifica que o alerta é disparado com os dados certos, sem bater
 * no Discord de verdade.
 */
@SpringBootTest
@Testcontainers
@Transactional
class DeadLetterListenerTest {

    @Container
    static MySQLContainer mysql = new MySQLContainer("mysql:8.0");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @MockitoBean
    DiscordAlertService discordAlertService;

    @Autowired
    DeadLetterListener deadLetterListener;

    @Autowired
    TenantRepository tenantRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    BatchItemRepository batchItemRepository;

    private static final AtomicInteger SEQUENCE = new AtomicInteger(0);

    UUID tenantId;
    UUID productId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        productId = UUID.randomUUID();

        Tenant tenant = new Tenant(
                tenantId, "Tenant Teste",
                String.format("%014d", SEQUENCE.incrementAndGet()),
                Tenant.Plan.FREE, Tenant.TenantStatus.ACTIVE, LocalDateTime.now());
        tenantRepository.save(tenant);

        Product product = new Product(productId, tenantId, "SKU-TESTE", "Notebook Dell",
                10, new BigDecimal("50.00"), LocalDateTime.now());
        productRepository.save(product);
    }

    @Test
    void handle_falhaRealAposTentativas_enviaAlertaComContagemDeAttempts() {
        BatchItem item = new BatchItem(UUID.randomUUID(), UUID.randomUUID(), productId,
                new BigDecimal("79.90"), 3);
        // Simula esgotamento de retries: 4 tentativas reais antes da DLQ,
        // mesmo número configurado no RetryConfig (maxAttempts: 4).
        item.markFailed("Erro simulado");
        item.markFailed("Erro simulado");
        item.markFailed("Erro simulado");
        item.markFailed("Erro simulado");
        batchItemRepository.save(item);

        SyncMessage message = new SyncMessage(item.getId(), productId, tenantId,
                new BigDecimal("79.90"), 3);

        deadLetterListener.handle(message);

        BatchItem updated = batchItemRepository.findById(item.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(BatchItem.BatchItemStatus.DEAD_LETTER);

        verify(discordAlertService).sendDeadLetterAlert(
                eq("Notebook Dell"), eq("Tenant Teste"), eq(item.getId().toString()),
                eq(4), eq(false));
    }

    @Test
    void handle_bloqueioPreventivoDoCircuitBreaker_enviaAlertaSemTentativas() {
        // attemptCount == 0: item nunca chegou a tentar de verdade, o
        // circuit breaker bloqueou antes de qualquer request de rede sair
        // — mesmo cenário coberto no SyncListenerTest.
        BatchItem item = new BatchItem(UUID.randomUUID(), UUID.randomUUID(), productId,
                new BigDecimal("79.90"), 3);
        batchItemRepository.save(item);

        SyncMessage message = new SyncMessage(item.getId(), productId, tenantId,
                new BigDecimal("79.90"), 3);

        deadLetterListener.handle(message);

        BatchItem updated = batchItemRepository.findById(item.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(BatchItem.BatchItemStatus.DEAD_LETTER);

        verify(discordAlertService).sendDeadLetterAlert(
                eq("Notebook Dell"), eq("Tenant Teste"), eq(item.getId().toString()),
                eq(0), eq(true));
    }
}