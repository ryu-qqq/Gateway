package com.ryuqq.gateway.application.tenant.port.out.client;

import com.ryuqq.gateway.domain.tenant.aggregate.TenantConfig;
import reactor.core.publisher.Mono;

/**
 * AuthHub Tenant Config 조회 Client Port (Out)
 *
 * <p>AuthHub API에서 Tenant Config를 조회하는 Outbound Port
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>AuthHub API 호출 (GET /api/v1/tenants/{tenantId}/config)
 *   <li>응답을 TenantConfig 도메인 모델로 변환
 * </ul>
 *
 * <p><strong>구현체</strong>:
 *
 * <ul>
 *   <li>AuthClientAdapter (adapter-out.authhub-client)
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public interface AuthClient {

    /**
     * AuthHub API에서 Tenant Config 조회
     *
     * @param tenantId 테넌트 ID
     * @return Mono&lt;TenantConfig&gt;
     */
    Mono<TenantConfig> fetchTenantConfig(String tenantId);
}
