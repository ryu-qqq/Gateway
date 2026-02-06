package com.ryuqq.gateway.application.tenant.manager;

import com.ryuqq.gateway.application.tenant.port.out.command.TenantConfigCommandPort;
import com.ryuqq.gateway.domain.tenant.aggregate.TenantConfig;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Tenant Config Command Manager (Reactive)
 *
 * <p>Redis Cache에 Tenant Config를 저장하거나 삭제하는 Manager
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>Redis Cache에 Tenant Config 저장
 *   <li>Redis Cache에서 Tenant Config 삭제 (캐시 무효화)
 * </ul>
 *
 * <p><strong>의존성</strong>:
 *
 * <ul>
 *   <li>TenantConfigCommandPort - Redis Cache 저장/삭제
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class TenantConfigCommandManager {

    private final TenantConfigCommandPort tenantConfigCommandPort;

    public TenantConfigCommandManager(TenantConfigCommandPort tenantConfigCommandPort) {
        this.tenantConfigCommandPort = tenantConfigCommandPort;
    }

    /**
     * Tenant Config 저장 (Redis Cache)
     *
     * @param tenantConfig 저장할 Tenant Config
     * @return Mono&lt;Void&gt;
     */
    public Mono<Void> save(TenantConfig tenantConfig) {
        return tenantConfigCommandPort.save(tenantConfig);
    }

    /**
     * Tenant Config 삭제 (캐시 무효화)
     *
     * @param tenantId 삭제할 테넌트 ID
     * @return Mono&lt;Void&gt;
     */
    public Mono<Void> deleteByTenantId(String tenantId) {
        return tenantConfigCommandPort.deleteByTenantId(tenantId);
    }
}
