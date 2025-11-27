package com.ryuqq.gateway.application.tenant.dto.response;

import com.ryuqq.gateway.domain.tenant.TenantConfig;

/**
 * Tenant Config 조회 응답 DTO
 *
 * <p>Tenant Config 조회 결과를 담는 Response 객체
 *
 * <p><strong>포함 정보</strong>:
 *
 * <ul>
 *   <li>tenantConfig: 조회된 Tenant Config Aggregate
 * </ul>
 *
 * @param tenantConfig 조회된 Tenant Config (null 불가)
 */
public record GetTenantConfigResponse(TenantConfig tenantConfig) {

    /** Compact Constructor - 검증 로직 */
    public GetTenantConfigResponse {
        if (tenantConfig == null) {
            throw new IllegalArgumentException("TenantConfig cannot be null");
        }
    }

    /**
     * TenantConfig로부터 Response 생성
     *
     * @param tenantConfig Tenant Config
     * @return GetTenantConfigResponse
     */
    public static GetTenantConfigResponse from(TenantConfig tenantConfig) {
        return new GetTenantConfigResponse(tenantConfig);
    }
}
