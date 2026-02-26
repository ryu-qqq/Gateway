package com.ryuqq.gateway.application.tenant.internal;

import com.ryuqq.gateway.application.tenant.manager.AuthClientManager;
import com.ryuqq.gateway.application.tenant.manager.TenantConfigCommandManager;
import com.ryuqq.gateway.application.tenant.manager.TenantConfigQueryManager;
import com.ryuqq.gateway.domain.tenant.aggregate.TenantConfig;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Tenant Config Coordinator (Reactive)
 *
 * <p>Redis Cache + AuthHub Fallback 전략을 조율하는 Coordinator
 *
 * <p><strong>Cache 전략</strong>:
 *
 * <ol>
 *   <li>TenantConfigQueryManager로 Redis Cache 조회
 *   <li>Cache Miss 시 AuthClientManager로 AuthHub API 호출
 *   <li>조회된 Tenant Config를 TenantConfigCommandManager로 Redis에 저장
 * </ol>
 *
 * <p><strong>의존성</strong>:
 *
 * <ul>
 *   <li>TenantConfigQueryManager - Redis Cache 조회
 *   <li>AuthClientManager - AuthHub API 호출 (Cache Miss Fallback)
 *   <li>TenantConfigCommandManager - Redis Cache 저장
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class TenantConfigCoordinator {

    private final TenantConfigQueryManager tenantConfigQueryManager;
    private final AuthClientManager authClientManager;
    private final TenantConfigCommandManager tenantConfigCommandManager;

    public TenantConfigCoordinator(
            TenantConfigQueryManager tenantConfigQueryManager,
            AuthClientManager authClientManager,
            TenantConfigCommandManager tenantConfigCommandManager) {
        this.tenantConfigQueryManager = tenantConfigQueryManager;
        this.authClientManager = authClientManager;
        this.tenantConfigCommandManager = tenantConfigCommandManager;
    }

    /**
     * Tenant Config 조회 (Cache Hit/Miss 전략)
     *
     * <p>Redis Cache에서 먼저 조회하고, Cache Miss 시 AuthHub API를 호출합니다.
     *
     * @param tenantId Tenant ID
     * @return Mono&lt;TenantConfig&gt;
     */
    public Mono<TenantConfig> findByTenantId(String tenantId) {
        return tenantConfigQueryManager
                .findByTenantId(tenantId)
                .switchIfEmpty(Mono.defer(() -> fetchFromAuthHubAndCache(tenantId)));
    }

    /**
     * AuthHub에서 Tenant Config 조회 후 Redis에 캐싱
     *
     * @param tenantId Tenant ID
     * @return Mono&lt;TenantConfig&gt;
     */
    private Mono<TenantConfig> fetchFromAuthHubAndCache(String tenantId) {
        return authClientManager
                .fetchTenantConfig(tenantId)
                .flatMap(
                        tenantConfig ->
                                tenantConfigCommandManager
                                        .save(tenantConfig)
                                        .thenReturn(tenantConfig));
    }
}
