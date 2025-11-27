package com.ryuqq.gateway.application.tenant.service.command;

import com.ryuqq.gateway.application.tenant.dto.command.SyncTenantConfigCommand;
import com.ryuqq.gateway.application.tenant.dto.response.SyncTenantConfigResponse;
import com.ryuqq.gateway.application.tenant.port.in.command.SyncTenantConfigUseCase;
import com.ryuqq.gateway.application.tenant.port.out.command.TenantConfigCommandPort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Tenant Config 동기화 Service
 *
 * <p>Webhook을 통한 Tenant Config 캐시 무효화를 수행하는 서비스
 *
 * <p><strong>동작 흐름</strong>:
 *
 * <ol>
 *   <li>AuthHub에서 Tenant Config 변경 발생
 *   <li>AuthHub → Gateway Webhook 호출 (POST /internal/gateway/tenants/config-changed)
 *   <li>TenantConfigWebhookController → SyncTenantConfigService 호출
 *   <li>Redis Cache에서 해당 Tenant Config 삭제
 *   <li>다음 요청 시 AuthHub API 호출 → 새 Config 캐싱
 * </ol>
 *
 * <p><strong>의존성 방향</strong>:
 *
 * <pre>
 * SyncTenantConfigService (Application Service)
 *   ↓ (calls)
 * TenantConfigCommandPort (Application Out Port)
 *   ↓ (implemented by)
 * TenantConfigCommandAdapter (Infrastructure Adapter)
 * </pre>
 *
 * @author development-team
 * @since 1.0.0
 */
@Service
public class SyncTenantConfigService implements SyncTenantConfigUseCase {

    private final TenantConfigCommandPort tenantConfigCommandPort;

    public SyncTenantConfigService(TenantConfigCommandPort tenantConfigCommandPort) {
        this.tenantConfigCommandPort = tenantConfigCommandPort;
    }

    /**
     * Tenant Config 동기화 실행 (캐시 무효화)
     *
     * <p>Redis Cache에서 해당 Tenant의 Config를 삭제합니다.
     *
     * @param command SyncTenantConfigCommand (tenantId 포함)
     * @return Mono&lt;SyncTenantConfigResponse&gt; (동기화 결과)
     */
    @Override
    public Mono<SyncTenantConfigResponse> execute(SyncTenantConfigCommand command) {
        return invalidateTenantConfigCache(command.tenantId())
                .thenReturn(SyncTenantConfigResponse.success(command.tenantId()))
                .onErrorResume(e -> Mono.just(SyncTenantConfigResponse.failure(command.tenantId())));
    }

    /**
     * Tenant Config 캐시 무효화
     *
     * @param tenantId Tenant ID
     * @return Mono&lt;Void&gt;
     */
    private Mono<Void> invalidateTenantConfigCache(String tenantId) {
        return tenantConfigCommandPort.deleteByTenantId(tenantId);
    }
}
