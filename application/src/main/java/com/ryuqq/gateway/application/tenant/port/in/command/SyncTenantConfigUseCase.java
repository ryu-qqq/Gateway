package com.ryuqq.gateway.application.tenant.port.in.command;

import com.ryuqq.gateway.application.tenant.dto.command.SyncTenantConfigCommand;
import com.ryuqq.gateway.application.tenant.dto.response.SyncTenantConfigResponse;
import reactor.core.publisher.Mono;

/**
 * Tenant Config 동기화 UseCase (Command Port-In)
 *
 * <p>Webhook을 통한 Tenant Config 캐시 무효화를 수행하는 Inbound Port
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>Redis Cache에서 Tenant Config 삭제 (캐시 무효화)
 *   <li>SyncTenantConfigResponse로 결과 반환
 * </ul>
 *
 * <p><strong>구현체</strong>:
 *
 * <ul>
 *   <li>SyncTenantConfigService (application.tenant.service.command)
 * </ul>
 *
 * <p><strong>트리거</strong>:
 *
 * <ul>
 *   <li>AuthHub → Gateway Webhook (POST /internal/gateway/tenants/config-changed)
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public interface SyncTenantConfigUseCase {

    /**
     * Tenant Config 동기화 실행 (캐시 무효화)
     *
     * @param command SyncTenantConfigCommand (tenantId 포함)
     * @return Mono&lt;SyncTenantConfigResponse&gt; (동기화 결과)
     */
    Mono<SyncTenantConfigResponse> execute(SyncTenantConfigCommand command);
}
