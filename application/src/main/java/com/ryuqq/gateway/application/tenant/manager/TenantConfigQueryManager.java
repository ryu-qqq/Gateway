package com.ryuqq.gateway.application.tenant.manager;

import com.ryuqq.gateway.application.tenant.port.out.query.TenantConfigQueryPort;
import com.ryuqq.gateway.domain.tenant.aggregate.TenantConfig;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Tenant Config 조회 Manager (Reactive)
 *
 * <p>Redis Cache에서 Tenant Config를 조회하는 Manager
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>Redis Cache에서 Tenant Config 조회
 *   <li>Cache Hit 시 TenantConfig 반환
 *   <li>Cache Miss 시 빈 Mono 반환
 * </ul>
 *
 * <p><strong>의존성</strong>:
 *
 * <ul>
 *   <li>TenantConfigQueryPort - Redis Cache 조회
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class TenantConfigQueryManager {

    private final TenantConfigQueryPort tenantConfigQueryPort;

    public TenantConfigQueryManager(TenantConfigQueryPort tenantConfigQueryPort) {
        this.tenantConfigQueryPort = tenantConfigQueryPort;
    }

    /**
     * Tenant Config 조회 (Redis Cache)
     *
     * @param tenantId Tenant ID
     * @return Mono&lt;TenantConfig&gt; (Cache Miss 시 빈 Mono)
     */
    public Mono<TenantConfig> findByTenantId(String tenantId) {
        return tenantConfigQueryPort.findByTenantId(tenantId);
    }
}
