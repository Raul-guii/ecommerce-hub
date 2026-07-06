package com.raul.ecommercehub.api.tenant;

import com.raul.ecommercehub.api.auth.AuthenticatedPrincipal;
import com.raul.ecommercehub.shared.domain.Tenant;
import com.raul.ecommercehub.shared.repository.TenantRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.Session;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

public class TenantFilterInterceptor extends OncePerRequestFilter {

    private final TenantRepository tenantRepository;
    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;

    public TenantFilterInterceptor(TenantRepository tenantRepository,
                                   EntityManager entityManager,
                                   PlatformTransactionManager transactionManager) {
        this.tenantRepository = tenantRepository;
        this.entityManager = entityManager;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        UUID tenantId = resolveTenantId(request);

        if (tenantId == null) {
            chain.doFilter(request, response);
            return;
        }

        transactionTemplate.execute(status -> {
            tenantRepository.findById(tenantId)
                    .filter(Tenant::isActive)
                    .orElseThrow(() -> new TenantNotFoundOrInactiveException(tenantId));

            TenantContext.set(tenantId);
            entityManager.unwrap(Session.class)
                    .enableFilter("tenantFilter")
                    .setParameter("tenantId", tenantId.toString());
            try {
                chain.doFilter(request, response);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                TenantContext.clear();
            }
            return null;
        });
    }

    private UUID resolveTenantId(HttpServletRequest request) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedPrincipal principal) {
            return principal.tenantId();
        }
        return null;
    }
}