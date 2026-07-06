package com.raul.ecommercehub.api.tenant;

import com.raul.ecommercehub.shared.domain.Product;
import com.raul.ecommercehub.shared.repository.ProductRepository;
import com.raul.ecommercehub.shared.domain.Tenant;
import com.raul.ecommercehub.shared.repository.TenantRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the core promise of step 2: a query made while the tenant filter
 * is enabled for tenant A never returns a row belonging to tenant B — even
 * though both rows live in the same table, same database, same cluster.
 *
 * <p>Uses a real MySQL container (via Testcontainers) instead of H2, since
 * Flyway runs the actual production migrations against it — an in-memory
 * substitute could mask a syntax or type mismatch that only shows up on
 * real MySQL.
 */
@Testcontainers
@SpringBootTest
class TenantIsolationTest {

    @Container
    static MySQLContainer mysql = new MySQLContainer("mysql:8.0");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void queryNeverReturnsAnotherTenantsData() {
        Tenant tenantA = persistTenant("Tenant A");
        Tenant tenantB = persistTenant("Tenant B");

        persistProduct(tenantA.getId(), "SKU-A");
        persistProduct(tenantB.getId(), "SKU-B");

        enableTenantFilter(tenantA.getId());

        List<Product> visibleProducts = productRepository.findAll();

        assertThat(visibleProducts)
                .isNotEmpty()
                .allMatch(p -> p.getTenantId().equals(tenantA.getId()));
    }

    private Tenant persistTenant(String name) {
        Tenant tenant = new Tenant(
                UUID.randomUUID(),
                name,
                "cnpj-" + UUID.randomUUID(),
                Tenant.Plan.FREE,
                Tenant.TenantStatus.ACTIVE,
                LocalDateTime.now());
        return tenantRepository.save(tenant);
    }

    private void persistProduct(UUID tenantId, String sku) {
        Product product = new Product(
                UUID.randomUUID(), tenantId, sku, "Test product",
                10, BigDecimal.TEN, LocalDateTime.now());
        productRepository.save(product);
    }

    private void enableTenantFilter(UUID tenantId) {
        TenantContext.set(tenantId);
        entityManager.unwrap(Session.class)
                .enableFilter("tenantFilter")
                .setParameter("tenantId", tenantId);
    }
}
