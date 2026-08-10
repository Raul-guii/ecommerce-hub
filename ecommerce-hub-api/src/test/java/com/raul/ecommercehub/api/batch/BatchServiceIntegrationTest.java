package com.raul.ecommercehub.api.batch;

import com.raul.ecommercehub.shared.domain.Batch;
import com.raul.ecommercehub.shared.domain.Product;
import com.raul.ecommercehub.shared.domain.Tenant;
import com.raul.ecommercehub.shared.repository.BatchRepository;
import com.raul.ecommercehub.shared.repository.ProductRepository;
import com.raul.ecommercehub.shared.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Regression test for the UnexpectedRollbackException bug found in Etapa 8.
 * <p>
 * TenantFilterInterceptor wraps the whole HTTP request in a single physical
 * transaction (needed to keep the Hibernate tenant filter active with
 * open-in-view: false). Before the fix, BatchService.createBatch() was a
 * plain @Transactional (REQUIRED) method, so a business exception thrown
 * inside it (e.g. an invalid SKU) marked the *entire* outer transaction as
 * rollback-only — even though GlobalExceptionHandler had already written
 * the HTTP response by the time the outer transaction tried to commit.
 * <p>
 * The fix moved createBatch() to @Transactional(REQUIRES_NEW), isolating
 * batch creation in its own transaction and manually re-applying the
 * Hibernate tenantFilter (REQUIRES_NEW opens a new Session).
 * <p>
 * This test simulates the outer transaction the same way
 * TenantFilterInterceptor does, and proves it survives a business
 * exception thrown by the inner call.
 */
@SpringBootTest
@Testcontainers
class BatchServiceIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    // Keeps this test focused on transactional behavior — no real broker needed.
    @MockitoBean
    RabbitTemplate rabbitTemplate;

    @Autowired
    BatchService batchService;

    @Autowired
    TenantRepository tenantRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    BatchRepository batchRepository;

    @Autowired
    PlatformTransactionManager transactionManager;

    UUID tenantId;

    private static final AtomicInteger CNPJ_SEQUENCE = new AtomicInteger(0);

    private String uniqueCnpj() {
        return String.format("%014d", CNPJ_SEQUENCE.incrementAndGet());
    }

    @BeforeEach
    void setUp() {
        Tenant tenant = new Tenant(
                UUID.randomUUID(),
                "Loja Teste",
                uniqueCnpj(),
                Tenant.Plan.FREE,
                Tenant.TenantStatus.ACTIVE,
                LocalDateTime.now());
        tenantRepository.save(tenant);
        tenantId = tenant.getId();

        Product product = new Product(
                UUID.randomUUID(), tenantId, "SKU-VALIDO", "Produto Teste",
                50, new BigDecimal("49.90"), LocalDateTime.now());
        productRepository.save(product);
    }

    @Test
    void createBatch_comSkuInvalido_naoDeveDerrubarTransacaoExterna() {
        // Simula exatamente o que o TenantFilterInterceptor faz: abre uma
        // transação e chama a cadeia de filtros/controller dentro dela.
        TransactionTemplate outerTransaction = new TransactionTemplate(transactionManager);

        BatchRequest request = new BatchRequest(List.of(
                new BatchItemRequest("SKU-QUE-NAO-EXISTE", new BigDecimal("10.00"), 5)
        ));

        // Antes da correção (REQUIRES_NEW), isso lançava
        // UnexpectedRollbackException na transação externa em vez de deixar
        // a ProductSkuNotFoundException ser tratada normalmente.
        assertThatCode(() ->
                outerTransaction.execute(status -> {
                    try {
                        batchService.createBatch(tenantId, request);
                    } catch (ProductSkuNotFoundException expected) {
                        // esperado — o ponto do teste é que a transação
                        // EXTERNA (outerTransaction) sobreviva a isso
                    }
                    return null;
                })
        ).doesNotThrowAnyException();
    }

    @Test
    void createBatch_comSkuValido_persisteEDevolveAccepted() {
        BatchRequest request = new BatchRequest(List.of(
                new BatchItemRequest("SKU-VALIDO", new BigDecimal("59.90"), 8)
        ));

        BatchResponse response = batchService.createBatch(tenantId, request);

        assertThat(response.batchId()).isNotNull();
        assertThat(response.status()).isEqualTo(Batch.BatchStatus.PROCESSING);

        Batch saved = batchRepository.findById(response.batchId()).orElseThrow();
        assertThat(saved.getTenantId()).isEqualTo(tenantId);
        assertThat(saved.getTotalItems()).isEqualTo(1);
    }

    @Test
    void createBatch_comSkuValido_dentroDeTransacaoExterna_naoQuebra() {
        // Mesmo cenário do teste de sucesso, mas envolto na transação
        // externa — garante que o caminho feliz também não regrediu
        // com a mudança para REQUIRES_NEW.
        TransactionTemplate outerTransaction = new TransactionTemplate(transactionManager);

        BatchRequest request = new BatchRequest(List.of(
                new BatchItemRequest("SKU-VALIDO", new BigDecimal("59.90"), 8)
        ));

        BatchResponse response = outerTransaction.execute(status ->
                batchService.createBatch(tenantId, request));

        assertThat(response).isNotNull();
        assertThat(response.batchId()).isNotNull();
    }

}
