package com.ryuqq.gateway.domain.tenant.exception;

import com.ryuqq.gateway.domain.common.exception.DomainException;

/**
 * Tenant Config Persistence 예외
 *
 * <p>Redis Cache 또는 AuthHub API에서 Tenant Config 저장/조회/삭제 중 오류 발생 시 사용
 *
 * <p><strong>발생 조건</strong>:
 *
 * <ul>
 *   <li>Redis Cache 저장 실패
 *   <li>Redis Cache 조회 실패
 *   <li>Redis Cache 삭제 실패 (캐시 무효화)
 *   <li>AuthHub API 호출 실패
 * </ul>
 *
 * <p><strong>HTTP 응답</strong>:
 *
 * <ul>
 *   <li>Status Code: 500 INTERNAL_SERVER_ERROR
 *   <li>Error Code: TENANT-004
 *   <li>Message: "Failed to persist or retrieve tenant configuration."
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public final class TenantConfigPersistenceException extends DomainException {

    private final String tenantId;
    private final String operation;

    /**
     * Tenant Config Persistence 예외 생성
     *
     * @param tenantId 대상 Tenant ID
     * @param operation 실패한 작업 (save, delete, fetch)
     * @param cause 원인 예외
     * @author development-team
     * @since 1.0.0
     */
    public TenantConfigPersistenceException(String tenantId, String operation, Throwable cause) {
        super(
                TenantErrorCode.TENANT_CONFIG_PERSISTENCE_ERROR,
                buildDetail(tenantId, operation));
        this.tenantId = tenantId;
        this.operation = operation;
        initCause(cause);
    }

    /**
     * Tenant Config Persistence 예외 생성 (cause 없음)
     *
     * @param tenantId 대상 Tenant ID
     * @param operation 실패한 작업 (save, delete, fetch)
     * @author development-team
     * @since 1.0.0
     */
    public TenantConfigPersistenceException(String tenantId, String operation) {
        super(
                TenantErrorCode.TENANT_CONFIG_PERSISTENCE_ERROR,
                buildDetail(tenantId, operation));
        this.tenantId = tenantId;
        this.operation = operation;
    }

    /**
     * Tenant ID 반환
     *
     * @return 대상 Tenant ID
     * @author development-team
     * @since 1.0.0
     */
    public String tenantId() {
        return tenantId;
    }

    /**
     * 실패한 작업 반환
     *
     * @return 실패한 작업명 (save, delete, fetch)
     * @author development-team
     * @since 1.0.0
     */
    public String operation() {
        return operation;
    }

    private static String buildDetail(String tenantId, String operation) {
        return String.format("tenantId=%s, operation=%s", tenantId, operation);
    }
}
