package com.ryuqq.gateway.domain.tenant.aggregate;

import com.ryuqq.gateway.domain.tenant.exception.MfaRequiredException;
import com.ryuqq.gateway.domain.tenant.exception.SocialLoginNotAllowedException;
import com.ryuqq.gateway.domain.tenant.id.TenantId;
import com.ryuqq.gateway.domain.tenant.vo.SessionConfig;
import com.ryuqq.gateway.domain.tenant.vo.SocialProvider;
import com.ryuqq.gateway.domain.tenant.vo.TenantRateLimitConfig;
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
        if (tenantId == null) {
            throw new NullPointerException("tenantId cannot be null");
        }
        this.tenantId = tenantId;
        this.mfaRequired = mfaRequired;
        this.allowedSocialLogins =
                allowedSocialLogins != null ? Set.copyOf(allowedSocialLogins) : Set.of();
        this.roleHierarchy = roleHierarchy != null ? Map.copyOf(roleHierarchy) : Map.of();
        this.sessionConfig = sessionConfig != null ? sessionConfig : SessionConfig.defaultConfig();
        this.rateLimitConfig =
                rateLimitConfig != null ? rateLimitConfig : TenantRateLimitConfig.defaultConfig();
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
     */
    public static TenantConfig of(
            TenantId tenantId,
            boolean mfaRequired,
            Set<SocialProvider> allowedSocialLogins,
            Map<String, Set<String>> roleHierarchy,
            SessionConfig sessionConfig,
            TenantRateLimitConfig rateLimitConfig) {
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
     * @param mfaVerified JWT에서 추출한 MFA 인증 완료 여부
     * @throws MfaRequiredException MFA 필수이나 인증되지 않은 경우
     */
    public void validateMfa(Boolean mfaVerified) {
        if (mfaRequired && (mfaVerified == null || !mfaVerified)) {
            throw new MfaRequiredException(tenantId.value());
        }
    }

    /**
     * 소셜 로그인 제공자 허용 여부 확인 및 검증
     *
     * @param provider 확인할 소셜 로그인 제공자
     * @throws SocialLoginNotAllowedException 허용되지 않은 제공자인 경우
     */
    public void validateSocialLoginProvider(SocialProvider provider) {
        if (!isSocialLoginAllowed(provider)) {
            throw new SocialLoginNotAllowedException(tenantId.value(), provider.getCode());
        }
    }

    /**
     * 소셜 로그인 제공자 허용 여부 확인 (문자열)
     *
     * @param providerCode 확인할 소셜 로그인 제공자 코드
     * @throws SocialLoginNotAllowedException 허용되지 않은 제공자인 경우
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
     */
    public boolean isSocialLoginAllowed(SocialProvider provider) {
        if (allowedSocialLogins.isEmpty()) {
            return true;
        }
        return allowedSocialLogins.contains(provider);
    }

    /**
     * 역할에 해당하는 권한 조회
     *
     * @param role 역할 이름
     * @return 해당 역할의 권한 Set (없으면 빈 Set)
     */
    public Set<String> getPermissionsForRole(String role) {
        Set<String> permissions = roleHierarchy.get(role);
        return permissions != null ? Set.copyOf(permissions) : Set.of();
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public String getTenantIdValue() {
        return tenantId.value();
    }

    public boolean isMfaRequired() {
        return mfaRequired;
    }

    public Set<SocialProvider> getAllowedSocialLogins() {
        return allowedSocialLogins;
    }

    public Map<String, Set<String>> getRoleHierarchy() {
        return roleHierarchy;
    }

    public SessionConfig getSessionConfig() {
        return sessionConfig;
    }

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
                + "tenantId="
                + tenantId
                + ", mfaRequired="
                + mfaRequired
                + ", allowedSocialLogins="
                + allowedSocialLogins
                + ", sessionConfig="
                + sessionConfig
                + ", rateLimitConfig="
                + rateLimitConfig
                + '}';
    }
}
