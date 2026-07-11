package com.raul.ecommercehub.shared.repository;

import com.raul.ecommercehub.shared.domain.TenantIntegrationConfig;
import com.raul.ecommercehub.shared.domain.enums.MarketplaceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantIntegrationConfigRepository extends JpaRepository<TenantIntegrationConfig, UUID> {

    Optional<TenantIntegrationConfig> findByTenantIdAndMarketplace(UUID tenantId, MarketplaceType marketplace);
}