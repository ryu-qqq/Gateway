package com.ryuqq.gateway.application.tenant.dto.response;

/**
 * Tenant Config 동기화 응답 DTO
 *
 * <p>Webhook을 통한 Tenant Config 캐시 무효화 결과를 담는 Response 객체
 *
 * <p><strong>포함 정보</strong>:
 *
 * <ul>
 *   <li>success: 캐시 무효화 성공 여부
 *   <li>tenantId: 처리된 테넌트 ID
 * </ul>
 *
 * @param success 캐시 무효화 성공 여부
 * @param tenantId 처리된 테넌트 ID
 */
public record SyncTenantConfigResponse(boolean success, String tenantId) {

    /**
     * 성공 응답 생성
     *
     * @param tenantId 처리된 테넌트 ID
     * @return 성공 SyncTenantConfigResponse
     */
    public static SyncTenantConfigResponse success(String tenantId) {
        return new SyncTenantConfigResponse(true, tenantId);
    }

    /**
     * 실패 응답 생성
     *
     * @param tenantId 처리 실패한 테넌트 ID
     * @return 실패 SyncTenantConfigResponse
     */
    public static SyncTenantConfigResponse failure(String tenantId) {
        return new SyncTenantConfigResponse(false, tenantId);
    }
}
