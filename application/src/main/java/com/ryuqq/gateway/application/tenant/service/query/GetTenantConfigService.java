package com.ryuqq.gateway.application.tenant.service.query;

import com.ryuqq.gateway.application.tenant.dto.query.GetTenantConfigQuery;
import com.ryuqq.gateway.application.tenant.dto.response.GetTenantConfigResponse;
import com.ryuqq.gateway.application.tenant.internal.TenantConfigCoordinator;
import com.ryuqq.gateway.application.tenant.port.in.query.GetTenantConfigUseCase;
import com.ryuqq.gateway.domain.tenant.aggregate.TenantConfig;
import com.ryuqq.gateway.domain.tenant.exception.TenantConfigPersistenceException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Tenant Config 조회 Service
 *
 * <p>TenantConfigCoordinator를 통해 Tenant Config를 조회하는 서비스
 *
 * <p><strong>의존성 방향</strong>:
 *
 * <pre>
 * GetTenantConfigService (Application Service)
 *   ↓ (calls)
 * TenantConfigCoordinator (Application Manager - internal)
 *   ↓ (calls)
 * TenantConfigQueryManager + AuthClientManager + TenantConfigCommandManager
 * </pre>
 *
 * @author development-team
 * @since 1.0.0
 */
@Service
public class GetTenantConfigService implements GetTenantConfigUseCase {

    private final TenantConfigCoordinator tenantConfigCoordinator;

    public GetTenantConfigService(TenantConfigCoordinator tenantConfigCoordinator) {
        this.tenantConfigCoordinator = tenantConfigCoordinator;
    }

    /**
     * Tenant Config 조회 실행
     *
     * @param query GetTenantConfigQuery (tenantId 포함)
     * @return Mono&lt;GetTenantConfigResponse&gt; (Tenant Config 정보)
     */
    @Override
    public Mono<GetTenantConfigResponse> execute(GetTenantConfigQuery query) {
        return getTenantConfig(query.tenantId()).map(GetTenantConfigResponse::from);
    }

    /**
     * Tenant Config 조회
     *
     * @param tenantId Tenant ID
     * @return Mono&lt;TenantConfig&gt;
     */
    public Mono<TenantConfig> getTenantConfig(String tenantId) {
        return tenantConfigCoordinator
                .findByTenantId(tenantId)
                .onErrorResume(
                        e ->
                                Mono.error(
                                        new TenantConfigPersistenceException(
                                                tenantId, "fetch", e)));
    }
}
