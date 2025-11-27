package com.ryuqq.gateway.domain.tenant.exception;

import com.ryuqq.gateway.domain.common.exception.DomainException;
import java.util.Map;

/**
 * Tenant ID 불일치 예외
 *
 * <p>JWT의 tenantId와 요청의 tenantId가 일치하지 않을 때 발생하는 예외
 *
 * <p><strong>발생 조건</strong>:
 *
 * <ul>
 *   <li>JWT Claim의 tenantId와 요청 Header의 X-Tenant-Id가 다른 경우
 *   <li>다른 Tenant의 리소스에 접근하려는 시도
 * </ul>
 *
 * <p><strong>HTTP 응답</strong>:
 *
 * <ul>
 *   <li>Status Code: 403 FORBIDDEN
 *   <li>Error Code: TENANT-001
 *   <li>Message: "Tenant ID mismatch. Access denied."
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public final class TenantMismatchException extends DomainException {

    private final String expectedTenantId;
    private final String actualTenantId;

    /**
     * Tenant ID 불일치 예외 생성
     *
     * @param expectedTenantId JWT에서 추출한 예상 Tenant ID
     * @param actualTenantId 요청에서 추출한 실제 Tenant ID
     * @author development-team
     * @since 1.0.0
     */
    public TenantMismatchException(String expectedTenantId, String actualTenantId) {
        super(
                TenantErrorCode.TENANT_MISMATCH.getCode(),
                String.format(
                        "Tenant ID mismatch. Expected: %s, Actual: %s",
                        expectedTenantId, actualTenantId),
                Map.of(
                        "expectedTenantId", expectedTenantId,
                        "actualTenantId", actualTenantId));
        this.expectedTenantId = expectedTenantId;
        this.actualTenantId = actualTenantId;
    }

    /**
     * 기본 에러 메시지 사용
     *
     * @param tenantId 관련된 Tenant ID
     * @author development-team
     * @since 1.0.0
     */
    public TenantMismatchException(String tenantId) {
        super(
                TenantErrorCode.TENANT_MISMATCH.getCode(),
                TenantErrorCode.TENANT_MISMATCH.getMessage(),
                Map.of("tenantId", tenantId));
        this.expectedTenantId = tenantId;
        this.actualTenantId = null;
    }

    /**
     * 예상 Tenant ID 반환
     *
     * @return JWT에서 추출한 Tenant ID
     * @author development-team
     * @since 1.0.0
     */
    public String getExpectedTenantId() {
        return expectedTenantId;
    }

    /**
     * 실제 Tenant ID 반환
     *
     * @return 요청에서 추출한 Tenant ID (null일 수 있음)
     * @author development-team
     * @since 1.0.0
     */
    public String getActualTenantId() {
        return actualTenantId;
    }
}
