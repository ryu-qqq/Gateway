package com.ryuqq.gateway.application.tenant.port.in.query;

import com.ryuqq.gateway.application.tenant.dto.query.GetTenantConfigQuery;
import com.ryuqq.gateway.application.tenant.dto.response.GetTenantConfigResponse;
import reactor.core.publisher.Mono;

/**
 * Tenant Config 조회 UseCase (Query Port-In)
 *
 * <p>Tenant Config를 조회하는 Inbound Port
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>Redis Cache에서 Tenant Config 조회 (Cache Hit)
 *   <li>Cache Miss 시 AuthHub API 호출 후 캐싱
 *   <li>GetTenantConfigResponse로 변환
 * </ul>
 *
 * <p><strong>구현체</strong>:
 *
 * <ul>
 *   <li>GetTenantConfigService (application.tenant.service.query)
 * </ul>
 *
 * <p><strong>사용처</strong>:
 *
 * <ul>
 *   <li>TenantIsolationFilter - Tenant 격리 검증
 *   <li>MfaRequiredFilter - MFA 필수 여부 확인
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public interface GetTenantConfigUseCase {

    /**
     * Tenant Config 조회 실행
     *
     * @param query GetTenantConfigQuery (tenantId 포함)
     * @return Mono&lt;GetTenantConfigResponse&gt; (Tenant Config 정보)
     */
    Mono<GetTenantConfigResponse> execute(GetTenantConfigQuery query);
}
