package com.raul.ecommercehub.worker.listener;

import com.raul.ecommercehub.shared.domain.BatchItem;
import com.raul.ecommercehub.shared.domain.Product;
import com.raul.ecommercehub.shared.messaging.SyncMessage;
import com.raul.ecommercehub.shared.repository.BatchItemRepository;
import com.raul.ecommercehub.shared.repository.ProductRepository;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Cobre o SyncListener chamando handle() diretamente (sem RabbitMQ real).
 * <p>
 * IMPORTANTE — o que este teste NÃO cobre: o retry com backoff exponencial
 * (SimpleRabbitListenerContainerFactory + RetryOperationsInterceptor) só é
 * aplicado quando a mensagem é consumida de uma fila real — chamar handle()
 * direto pula esse mecanismo inteiro. Testar o retry de verdade exigiria um
 * Testcontainer RabbitMQ e ~15-20s de espera real (2s+4s+8s de backoff antes
 * de esgotar as 4 tentativas). Ficou de fora por decisão consciente, para
 * priorizar velocidade nesta rodada — ver estado-atual-e-proximos-passos.md.
 * <p>
 * O que este teste cobre: o circuit breaker (via chamada direta ao
 * MarketplaceSyncExecutor, que usa AOP normal do Spring — funciona mesmo
 * sem RabbitMQ), a persistência de status do BatchItem, e a atualização do
 * Product em caso de sucesso.
 */
@SpringBootTest
@Testcontainers
@org.springframework.transaction.annotation.Transactional
class SyncListenerTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    static WireMockServer wireMock = new WireMockServer(0); // porta dinâmica
    static {
        wireMock.start();
    }


    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("marketplace.base-url", () -> "http://localhost:" + wireMock.port());
    }

    @MockitoBean
    RedisTemplate<String, Object> redisTemplate;;

    @Autowired
    SyncListener syncListener;

    @Autowired
    BatchItemRepository batchItemRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    CircuitBreakerRegistry circuitBreakerRegistry;

    @PersistenceContext
    EntityManager entityManager;

    private static final AtomicInteger SEQUENCE = new AtomicInteger(0);

    UUID tenantId;
    UUID productId;

    @BeforeEach
    void setUp() {
        if (!wireMock.isRunning()) {
            wireMock.start();
        }
        wireMock.resetAll();

        // RedisTemplate mockado sempre em MISS — força buscar a credencial
        // do MySQL toda vez, sem precisar de um Testcontainer Redis.
        ValueOperations<String, Object> valueOps = org.mockito.Mockito.mock(ValueOperations.class);
        org.mockito.Mockito.when(redisTemplate.opsForValue()).thenReturn(valueOps);
        org.mockito.Mockito.when(valueOps.get(org.mockito.ArgumentMatchers.anyString())).thenReturn(null);

        tenantId = UUID.randomUUID();
        productId = UUID.randomUUID();

        entityManager.createNativeQuery("""
        INSERT INTO tenants (id, name, cnpj, plan, status, created_at)
        VALUES (?, 'Tenant Teste', ?, 'FREE', 'ACTIVE', ?)
        """)
                .setParameter(1, tenantId.toString())
                .setParameter(2, tenantId.toString().substring(0, 14).replace("-", ""))
                .setParameter(3, LocalDateTime.now())
                .executeUpdate();

        Product product = new Product(productId, tenantId, "SKU-TESTE", "Produto Teste",
                10, new BigDecimal("50.00"), LocalDateTime.now());


        productRepository.save(product);

        // TenantIntegrationConfig: o construtor Java não seta tenantId (não
        // chama super(tenantId)) — mesmo gap que exigiu INSERT manual na
        // Etapa 8. Contornado aqui via SQL nativo até o construtor ser corrigido.
        entityManager.createNativeQuery("""
                INSERT INTO tenant_integration_configs
                (id, tenant_id, marketplace, access_token_encrypted, refresh_token_encrypted, token_expires_at)
                VALUES (?, ?, 'AMAZON', 'fake-token-for-testing', NULL, ?)
                """)
                .setParameter(1, UUID.randomUUID().toString())
                .setParameter(2, tenantId.toString())
                .setParameter(3, LocalDateTime.now().plusYears(1))
                .executeUpdate();

        // Garante circuito fechado no início de cada teste, independente
        // do que o teste anterior deixou.
        circuitBreakerRegistry.circuitBreaker("marketplaceSync").reset();
    }

    @AfterEach
    void tearDown() {
        circuitBreakerRegistry.circuitBreaker("marketplaceSync").reset();
    }

    private UUID nextBatchItemId(UUID batchId) {
        BatchItem item = new BatchItem(UUID.randomUUID(), batchId, productId,
                new BigDecimal("59.90"), 8);
        batchItemRepository.save(item);
        return item.getId();
    }

    @Test
    void handle_marketplaceRespondeComSucesso_marcaBatchItemComoSuccess() {
        wireMock.stubFor(post(urlEqualTo("/marketplace/sync"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"SYNCED\"}")));

        UUID batchId = UUID.randomUUID();
        UUID batchItemId = nextBatchItemId(batchId);
        SyncMessage message = new SyncMessage(batchItemId, productId, tenantId,
                new BigDecimal("79.90"), 3);

        syncListener.handle(message);

        BatchItem saved = batchItemRepository.findById(batchItemId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(BatchItem.BatchItemStatus.SUCCESS);

        Product updatedProduct = productRepository.findById(productId).orElseThrow();
        assertThat(updatedProduct.getPrice()).isEqualByComparingTo("79.90");
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(3);
    }

    @Test
    void handle_marketplaceRespondeComErro_marcaBatchItemComoFailedERelancaExcecao() {
        wireMock.stubFor(post(urlEqualTo("/marketplace/sync"))
                .willReturn(aResponse().withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"Internal Server Error\"}")));

        UUID batchId = UUID.randomUUID();
        UUID batchItemId = nextBatchItemId(batchId);
        SyncMessage message = new SyncMessage(batchItemId, productId, tenantId,
                new BigDecimal("79.90"), 3);

        assertThatThrownBy(() -> syncListener.handle(message))
                .isInstanceOf(Exception.class);

        BatchItem saved = batchItemRepository.findById(batchItemId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(BatchItem.BatchItemStatus.FAILED);
        assertThat(saved.getAttemptCount()).isEqualTo(1);
        assertThat(saved.getLastError()).isNotBlank();
    }

    @Test
    void handle_circuitoAberto_bloqueiaChamadaSemBaterNoMarketplace() {
        // Força o circuito pro estado OPEN, simulando o cenário real da
        // Etapa 8: itens indo pra dead_letter sem nenhuma tentativa real,
        // porque o marketplace já foi considerado instável antes.
        circuitBreakerRegistry.circuitBreaker("marketplaceSync")
                .transitionToOpenState();

        wireMock.stubFor(post(urlEqualTo("/marketplace/sync"))
                .willReturn(aResponse().withStatus(200))); // não deveria nem ser chamado

        UUID batchId = UUID.randomUUID();
        UUID batchItemId = nextBatchItemId(batchId);
        SyncMessage message = new SyncMessage(batchItemId, productId, tenantId,
                new BigDecimal("79.90"), 3);

        assertThatThrownBy(() -> syncListener.handle(message))
                .isInstanceOf(Exception.class);

        BatchItem saved = batchItemRepository.findById(batchItemId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(BatchItem.BatchItemStatus.FAILED);

        // Confirma que o WireMock nunca recebeu a chamada — o circuito
        // bloqueou antes de qualquer request de rede sair.
        wireMock.verify(0, WireMock.postRequestedFor(urlEqualTo("/marketplace/sync")));
    }
}