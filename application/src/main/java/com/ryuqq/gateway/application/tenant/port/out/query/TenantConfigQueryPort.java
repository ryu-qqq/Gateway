package com.ryuqq.gateway.application.tenant.port.out.query;

import com.ryuqq.gateway.domain.tenant.aggregate.TenantConfig;
import reactor.core.publisher.Mono;

/**
 * Tenant Config 조회 Query Port (Out)
 *
 * <p>Redis Cache에서 Tenant Config를 조회하는 Outbound Port
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>Redis Cache에서 Tenant Config 조회
 *   <li>Cache Hit 시 TenantConfig 반환
 *   <li>Cache Miss 시 빈 Mono 반환
 * </ul>
 *
 * <p><strong>구현체</strong>:
 *
 * <ul>
 *   <li>TenantConfigQueryAdapter (adapter-out.persistence-redis)
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public interface TenantConfigQueryPort {

    /**
     * Tenant Config 조회 (Redis Cache)
     *
     * @param tenantId 테넌트 ID
     * @return Mono&lt;TenantConfig&gt; (Cache Miss 시 빈 Mono)
     */
    Mono<TenantConfig> findByTenantId(String tenantId);
}
