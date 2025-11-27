package com.ryuqq.gateway.domain.tenant.exception;

import com.ryuqq.gateway.domain.common.exception.DomainException;

import java.util.Map;

/**
 * 소셜 로그인 제공자 불허용 예외
 *
 * <p>Tenant Config에서 허용되지 않은 소셜 로그인 제공자로 로그인 시도한 경우 발생
 *
 * <p><strong>발생 조건</strong>:
 *
 * <ul>
 *   <li>TenantConfig.allowedSocialLogins에 요청한 provider가 없는 경우
 *   <li>예: allowedSocialLogins = [KAKAO], 요청 provider = NAVER
 * </ul>
 *
 * <p><strong>HTTP 응답</strong>:
 *
 * <ul>
 *   <li>Status Code: 403 FORBIDDEN
 *   <li>Error Code: TENANT-003
 *   <li>Message: "Social login provider is not allowed for this tenant."
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public final class SocialLoginNotAllowedException extends DomainException {

    private final String tenantId;
    private final String provider;

    /**
     * 소셜 로그인 불허용 예외 생성
     *
     * @param tenantId Tenant ID
     * @param provider 허용되지 않은 소셜 로그인 제공자
     * @author development-team
     * @since 1.0.0
     */
    public SocialLoginNotAllowedException(String tenantId, String provider) {
        super(
                TenantErrorCode.SOCIAL_LOGIN_NOT_ALLOWED.getCode(),
                String.format(
                        "Social login provider '%s' is not allowed for tenant: %s",
                        provider,
                        tenantId),
                Map.of(
                        "tenantId", tenantId,
                        "provider", provider));
        this.tenantId = tenantId;
        this.provider = provider;
    }

    /**
     * 기본 에러 메시지 사용
     *
     * @param provider 허용되지 않은 소셜 로그인 제공자
     * @author development-team
     * @since 1.0.0
     */
    public SocialLoginNotAllowedException(String provider) {
        super(
                TenantErrorCode.SOCIAL_LOGIN_NOT_ALLOWED.getCode(),
                String.format(
                        "Social login provider '%s' is not allowed for this tenant.",
                        provider),
                Map.of("provider", provider));
        this.tenantId = null;
        this.provider = provider;
    }

    /**
     * Tenant ID 반환
     *
     * @return Tenant ID (null일 수 있음)
     * @author development-team
     * @since 1.0.0
     */
    public String getTenantId() {
        return tenantId;
    }

    /**
     * 소셜 로그인 제공자 반환
     *
     * @return 허용되지 않은 소셜 로그인 제공자
     * @author development-team
     * @since 1.0.0
     */
    public String getProvider() {
        return provider;
    }
}
