package com.ryuqq.gateway.domain.tenant;

import com.ryuqq.gateway.domain.tenant.exception.MfaRequiredException;
import com.ryuqq.gateway.domain.tenant.exception.SocialLoginNotAllowedException;
import com.ryuqq.gateway.domain.tenant.vo.SessionConfig;
import com.ryuqq.gateway.domain.tenant.vo.SocialProvider;
import com.ryuqq.gateway.domain.tenant.vo.TenantId;
import com.ryuqq.gateway.domain.tenant.vo.TenantRateLimitConfig;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * TenantConfig - 테넌트 설정 Aggregate Root
 *
 * <p>멀티테넌트 시스템에서 테넌트별 설정을 관리하는 도메인 모델입니다.
 *
 * <p><strong>Aggregate 구성:</strong>
 *
 * <ul>
 *   <li>TenantId: 테넌트 식별자 (Value Object)
 *   <li>mfaRequired: MFA 필수 여부
 *   <li>allowedSocialLogins: 허용된 소셜 로그인 제공자 (Set)
 *   <li>roleHierarchy: 역할별 권한 매핑 (Map)
 *   <li>SessionConfig: 세션 설정 (Value Object)
 *   <li>TenantRateLimitConfig: Rate Limit 설정 (Value Object)
 * </ul>
 *
 * <p><strong>도메인 규칙:</strong>
 *
 * <ul>
 *   <li>MFA 필수인 경우 JWT의 mfaVerified=true 필요
 *   <li>소셜 로그인은 allowedSocialLogins에 포함된 제공자만 허용
 *   <li>역할 계층 구조에 따른 권한 상속 지원
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public final class TenantConfig {

    private final TenantId tenantId;
    private final boolean mfaRequired;
    private final Set<SocialProvider> allowedSocialLogins;
    private final Map<String, Set<String>> roleHierarchy;
    private final SessionConfig sessionConfig;
    private final TenantRateLimitConfig rateLimitConfig;

    private TenantConfig(
            TenantId tenantId,
            boolean mfaRequired,
            Set<SocialProvider> allowedSocialLogins,
            Map<String, Set<String>> roleHierarchy,
            SessionConfig sessionConfig,
            TenantRateLimitConfig rateLimitConfig) {
        this.tenantId = tenantId;
        this.mfaRequired = mfaRequired;
        this.allowedSocialLogins = allowedSocialLogins != null
                ? Set.copyOf(allowedSocialLogins)
                : Set.of();
        this.roleHierarchy = roleHierarchy != null
                ? Map.copyOf(roleHierarchy)
                : Map.of();
        this.sessionConfig = sessionConfig != null
                ? sessionConfig
                : SessionConfig.defaultConfig();
        this.rateLimitConfig = rateLimitConfig != null
                ? rateLimitConfig
                : TenantRateLimitConfig.defaultConfig();
    }

    /**
     * TenantConfig 생성 (전체 필드)
     *
     * @param tenantId 테넌트 ID
     * @param mfaRequired MFA 필수 여부
     * @param allowedSocialLogins 허용된 소셜 로그인 제공자
     * @param roleHierarchy 역할별 권한 매핑
     * @param sessionConfig 세션 설정
     * @param rateLimitConfig Rate Limit 설정
     * @return TenantConfig 인스턴스
     * @throws NullPointerException tenantId가 null인 경우
     * @author development-team
     * @since 1.0.0
     */
    public static TenantConfig of(
            TenantId tenantId,
            boolean mfaRequired,
            Set<SocialProvider> allowedSocialLogins,
            Map<String, Set<String>> roleHierarchy,
            SessionConfig sessionConfig,
            TenantRateLimitConfig rateLimitConfig) {
        Objects.requireNonNull(tenantId, "tenantId cannot be null");

        return new TenantConfig(
                tenantId,
                mfaRequired,
                allowedSocialLogins,
                roleHierarchy,
                sessionConfig,
                rateLimitConfig);
    }

    /**
     * TenantConfig 생성 (필수 필드만)
     *
     * @param tenantId 테넌트 ID
     * @param mfaRequired MFA 필수 여부
     * @return TenantConfig 인스턴스
     * @author development-team
     * @since 1.0.0
     */
    public static TenantConfig of(TenantId tenantId, boolean mfaRequired) {
        return of(tenantId, mfaRequired, Set.of(), Map.of(), null, null);
    }

    /**
     * 문자열 tenantId로 TenantConfig 생성
     *
     * @param tenantIdValue 테넌트 ID 문자열
     * @param mfaRequired MFA 필수 여부
     * @param allowedSocialLogins 허용된 소셜 로그인 제공자
     * @param roleHierarchy 역할별 권한 매핑
     * @param sessionConfig 세션 설정
     * @param rateLimitConfig Rate Limit 설정
     * @return TenantConfig 인스턴스
     * @author development-team
     * @since 1.0.0
     */
    public static TenantConfig of(
            String tenantIdValue,
            boolean mfaRequired,
            Set<SocialProvider> allowedSocialLogins,
            Map<String, Set<String>> roleHierarchy,
            SessionConfig sessionConfig,
            TenantRateLimitConfig rateLimitConfig) {
        return of(
                TenantId.from(tenantIdValue),
                mfaRequired,
                allowedSocialLogins,
                roleHierarchy,
                sessionConfig,
                rateLimitConfig);
    }

    /**
     * MFA 검증 필요 여부 확인 및 검증
     *
     * <p>MFA 필수인 경우 mfaVerified=false이면 예외를 발생시킵니다.
     *
     * @param mfaVerified JWT에서 추출한 MFA 인증 완료 여부
     * @throws MfaRequiredException MFA 필수이나 인증되지 않은 경우
     * @author development-team
     * @since 1.0.0
     */
    public void validateMfa(Boolean mfaVerified) {
        if (mfaRequired && (mfaVerified == null || !mfaVerified)) {
            throw new MfaRequiredException(tenantId.getValue());
        }
    }

    /**
     * 소셜 로그인 제공자 허용 여부 확인 및 검증
     *
     * <p>허용되지 않은 제공자인 경우 예외를 발생시킵니다.
     *
     * @param provider 확인할 소셜 로그인 제공자
     * @throws SocialLoginNotAllowedException 허용되지 않은 제공자인 경우
     * @author development-team
     * @since 1.0.0
     */
    public void validateSocialLoginProvider(SocialProvider provider) {
        if (!isSocialLoginAllowed(provider)) {
            throw new SocialLoginNotAllowedException(tenantId.getValue(), provider.getCode());
        }
    }

    /**
     * 소셜 로그인 제공자 허용 여부 확인 (문자열)
     *
     * @param providerCode 확인할 소셜 로그인 제공자 코드
     * @throws SocialLoginNotAllowedException 허용되지 않은 제공자인 경우
     * @author development-team
     * @since 1.0.0
     */
    public void validateSocialLoginProvider(String providerCode) {
        SocialProvider provider = SocialProvider.fromCode(providerCode);
        validateSocialLoginProvider(provider);
    }

    /**
     * 소셜 로그인 허용 여부 확인
     *
     * @param provider 확인할 소셜 로그인 제공자
     * @return 허용되면 true
     * @author development-team
     * @since 1.0.0
     */
    public boolean isSocialLoginAllowed(SocialProvider provider) {
        if (allowedSocialLogins.isEmpty()) {
            return true; // 빈 Set은 모든 제공자 허용으로 해석
        }
        return allowedSocialLogins.contains(provider);
    }

    /**
     * 역할에 해당하는 권한 조회
     *
     * @param role 역할 이름
     * @return 해당 역할의 권한 Set (없으면 빈 Set)
     * @author development-team
     * @since 1.0.0
     */
    public Set<String> getPermissionsForRole(String role) {
        Set<String> permissions = roleHierarchy.get(role);
        return permissions != null ? Set.copyOf(permissions) : Set.of();
    }

    /**
     * 테넌트 ID 반환
     *
     * @return TenantId Value Object
     * @author development-team
     * @since 1.0.0
     */
    public TenantId getTenantId() {
        return tenantId;
    }

    /**
     * 테넌트 ID 문자열 반환
     *
     * @return 테넌트 ID 문자열
     * @author development-team
     * @since 1.0.0
     */
    public String getTenantIdValue() {
        return tenantId.getValue();
    }

    /**
     * MFA 필수 여부 반환
     *
     * @return MFA 필수이면 true
     * @author development-team
     * @since 1.0.0
     */
    public boolean isMfaRequired() {
        return mfaRequired;
    }

    /**
     * 허용된 소셜 로그인 제공자 반환
     *
     * @return 허용된 소셜 로그인 제공자 Set (불변)
     * @author development-team
     * @since 1.0.0
     */
    public Set<SocialProvider> getAllowedSocialLogins() {
        return allowedSocialLogins;
    }

    /**
     * 역할별 권한 매핑 반환
     *
     * @return 역할별 권한 Map (불변)
     * @author development-team
     * @since 1.0.0
     */
    public Map<String, Set<String>> getRoleHierarchy() {
        return roleHierarchy;
    }

    /**
     * 세션 설정 반환
     *
     * @return SessionConfig Value Object
     * @author development-team
     * @since 1.0.0
     */
    public SessionConfig getSessionConfig() {
        return sessionConfig;
    }

    /**
     * Rate Limit 설정 반환
     *
     * @return TenantRateLimitConfig Value Object
     * @author development-team
     * @since 1.0.0
     */
    public TenantRateLimitConfig getRateLimitConfig() {
        return rateLimitConfig;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TenantConfig that = (TenantConfig) o;
        return Objects.equals(tenantId, that.tenantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenantId);
    }

    @Override
    public String toString() {
        return "TenantConfig{"
                + "tenantId=" + tenantId
                + ", mfaRequired=" + mfaRequired
                + ", allowedSocialLogins=" + allowedSocialLogins
                + ", sessionConfig=" + sessionConfig
                + ", rateLimitConfig=" + rateLimitConfig
                + '}';
    }
}
