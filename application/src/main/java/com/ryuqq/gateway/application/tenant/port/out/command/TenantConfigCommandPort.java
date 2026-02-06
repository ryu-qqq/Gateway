package com.ryuqq.gateway.application.tenant.port.out.command;

import com.ryuqq.gateway.domain.tenant.aggregate.TenantConfig;
import reactor.core.publisher.Mono;

/**
 * Tenant Config 저장/삭제 Command Port (Out)
 *
 * <p>Redis Cache에 Tenant Config를 저장하거나 삭제하는 Outbound Port
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>Redis Cache에 Tenant Config 저장 (TTL: 1시간)
 *   <li>Redis Cache에서 Tenant Config 삭제 (캐시 무효화)
 * </ul>
 *
 * <p><strong>구현체</strong>:
 *
 * <ul>
 *   <li>TenantConfigCommandAdapter (adapter-out.persistence-redis)
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public interface TenantConfigCommandPort {

    /**
     * Tenant Config 저장 (Redis Cache)
     *
     * @param tenantConfig 저장할 Tenant Config
     * @return Mono&lt;Void&gt;
     */
    Mono<Void> save(TenantConfig tenantConfig);

    /**
     * Tenant Config 삭제 (캐시 무효화)
     *
     * @param tenantId 삭제할 테넌트 ID
     * @return Mono&lt;Void&gt;
     */
    Mono<Void> deleteByTenantId(String tenantId);
}
