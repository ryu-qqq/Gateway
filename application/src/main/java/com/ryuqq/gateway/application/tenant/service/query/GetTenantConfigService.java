package com.ryuqq.gateway.application.tenant.service.query;

import com.ryuqq.gateway.application.tenant.dto.query.GetTenantConfigQuery;
import com.ryuqq.gateway.application.tenant.dto.response.GetTenantConfigResponse;
import com.ryuqq.gateway.application.tenant.port.in.query.GetTenantConfigUseCase;
import com.ryuqq.gateway.application.tenant.port.out.client.AuthHubTenantClient;
import com.ryuqq.gateway.application.tenant.port.out.command.TenantConfigCommandPort;
import com.ryuqq.gateway.application.tenant.port.out.query.TenantConfigQueryPort;
import com.ryuqq.gateway.domain.tenant.TenantConfig;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Tenant Config 조회 Service
 *
 * <p>Redis Cache + AuthHub Fallback 전략을 사용하여 Tenant Config를 조회하는 서비스
 *
 * <p><strong>Cache 전략</strong>:
 *
 * <ol>
 *   <li>Redis Cache에서 조회 (TenantConfigQueryPort)
 *   <li>Cache Miss 시 AuthHub API 호출 (AuthHubTenantClient)
 *   <li>조회된 Tenant Config를 Redis에 저장 (TenantConfigCommandPort)
 * </ol>
 *
 * <p><strong>의존성 방향</strong>:
 *
 * <pre>
 * GetTenantConfigService (Application Service)
 *   ↓ (calls)
 * TenantConfigQueryPort + AuthHubTenantClient + TenantConfigCommandPort (Application Out Ports)
 *   ↓ (implemented by)
 * TenantConfigQueryAdapter + AuthHubTenantClientAdapter + TenantConfigCommandAdapter (Infrastructure Adapters)
 * </pre>
 *
 * @author development-team
 * @since 1.0.0
 */
@Service
public class GetTenantConfigService implements GetTenantConfigUseCase {

    private final TenantConfigQueryPort tenantConfigQueryPort;
    private final AuthHubTenantClient authHubTenantClient;
    private final TenantConfigCommandPort tenantConfigCommandPort;

    public GetTenantConfigService(
            TenantConfigQueryPort tenantConfigQueryPort,
            AuthHubTenantClient authHubTenantClient,
            TenantConfigCommandPort tenantConfigCommandPort) {
        this.tenantConfigQueryPort = tenantConfigQueryPort;
        this.authHubTenantClient = authHubTenantClient;
        this.tenantConfigCommandPort = tenantConfigCommandPort;
    }

    /**
     * Tenant Config 조회 실행
     *
     * <p>Cache Hit/Miss 전략을 사용하여 Tenant Config를 조회합니다.
     *
     * @param query GetTenantConfigQuery (tenantId 포함)
     * @return Mono&lt;GetTenantConfigResponse&gt; (Tenant Config 정보)
     */
    @Override
    public Mono<GetTenantConfigResponse> execute(GetTenantConfigQuery query) {
        return getTenantConfig(query.tenantId())
                .map(GetTenantConfigResponse::from);
    }

    /**
     * Tenant Config 조회 (Cache Hit/Miss 전략)
     *
     * @param tenantId Tenant ID
     * @return Mono&lt;TenantConfig&gt;
     */
    public Mono<TenantConfig> getTenantConfig(String tenantId) {
        return tenantConfigQueryPort
                .findByTenantId(tenantId)
                .switchIfEmpty(fetchFromAuthHubAndCache(tenantId))
                .onErrorResume(
                        e -> Mono.error(
                                new RuntimeException(
                                        "Failed to get tenant config for tenantId: " + tenantId, e)));
    }

    /**
     * AuthHub에서 Tenant Config 조회 후 Redis에 캐싱
     *
     * @param tenantId Tenant ID
     * @return Mono&lt;TenantConfig&gt;
     */
    private Mono<TenantConfig> fetchFromAuthHubAndCache(String tenantId) {
        return authHubTenantClient
                .fetchTenantConfig(tenantId)
                .flatMap(tenantConfig ->
                        tenantConfigCommandPort
                                .save(tenantConfig)
                                .thenReturn(tenantConfig));
    }
}
