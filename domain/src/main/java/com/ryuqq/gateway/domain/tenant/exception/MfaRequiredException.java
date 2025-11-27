package com.ryuqq.gateway.domain.tenant.exception;

import com.ryuqq.gateway.domain.common.exception.DomainException;
import java.util.Map;

/**
 * MFA 인증 필수 예외
 *
 * <p>Tenant Config에서 MFA 필수로 설정되었으나, 사용자가 MFA 인증을 완료하지 않은 경우 발생
 *
 * <p><strong>발생 조건</strong>:
 *
 * <ul>
 *   <li>TenantConfig.mfaRequired = true
 *   <li>JWT Claim의 mfaVerified = false 또는 없음
 * </ul>
 *
 * <p><strong>HTTP 응답</strong>:
 *
 * <ul>
 *   <li>Status Code: 403 FORBIDDEN
 *   <li>Error Code: TENANT-002
 *   <li>Message: "Multi-Factor Authentication is required for this tenant."
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public final class MfaRequiredException extends DomainException {

    private final String tenantId;

    /**
     * MFA 필수 예외 생성
     *
     * @param tenantId MFA가 필수인 Tenant ID
     * @author development-team
     * @since 1.0.0
     */
    public MfaRequiredException(String tenantId) {
        super(
                TenantErrorCode.MFA_REQUIRED.getCode(),
                String.format("MFA verification required for tenant: %s", tenantId),
                Map.of("tenantId", tenantId));
        this.tenantId = tenantId;
    }

    /**
     * 기본 에러 메시지 사용
     *
     * @author development-team
     * @since 1.0.0
     */
    public MfaRequiredException() {
        super(TenantErrorCode.MFA_REQUIRED.getCode(), TenantErrorCode.MFA_REQUIRED.getMessage());
        this.tenantId = null;
    }

    /**
     * Tenant ID 반환
     *
     * @return MFA가 필수인 Tenant ID
     * @author development-team
     * @since 1.0.0
     */
    public String getTenantId() {
        return tenantId;
    }
}
