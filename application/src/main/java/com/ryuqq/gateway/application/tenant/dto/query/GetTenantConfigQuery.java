package com.ryuqq.gateway.application.tenant.dto.query;

/**
 * Tenant Config 조회 Query DTO
 *
 * <p>Tenant Config를 조회하기 위한 Query 객체
 *
 * <p><strong>검증 규칙</strong>:
 *
 * <ul>
 *   <li>tenantId는 null 또는 blank일 수 없다
 * </ul>
 *
 * @param tenantId 조회할 테넌트 ID (null/blank 불가)
 */
public record GetTenantConfigQuery(String tenantId) {

    /** Compact Constructor - 검증 로직 */
    public GetTenantConfigQuery {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("TenantId cannot be null or blank");
        }
    }
}
