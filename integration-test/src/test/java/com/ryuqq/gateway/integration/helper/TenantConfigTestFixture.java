package com.ryuqq.gateway.integration.helper;

import java.util.List;

/**
 * Tenant Config Test Fixture
 *
 * <p>Integration Test를 위한 Tenant Config 데이터 생성 유틸리티
 *
 * @author development-team
 * @since 1.0.0
 */
public final class TenantConfigTestFixture {

    private TenantConfigTestFixture() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 기본 Tenant Config 응답 생성
     *
     * @param tenantId 테넌트 ID
     * @return Tenant Config JSON 문자열
     */
    public static String tenantConfigResponse(String tenantId) {
        return tenantConfigResponse(tenantId, false, List.of("KAKAO", "NAVER", "GOOGLE"));
    }

    /**
     * MFA 필수 Tenant Config 응답 생성
     *
     * @param tenantId 테넌트 ID
     * @param mfaRequired MFA 필수 여부
     * @return Tenant Config JSON 문자열
     */
    public static String tenantConfigResponse(String tenantId, boolean mfaRequired) {
        return tenantConfigResponse(tenantId, mfaRequired, List.of("KAKAO", "NAVER", "GOOGLE"));
    }

    /**
     * 소셜 로그인 제한된 Tenant Config 응답 생성
     *
     * @param tenantId 테넌트 ID
     * @param mfaRequired MFA 필수 여부
     * @param allowedSocialLogins 허용된 소셜 로그인 목록
     * @return Tenant Config JSON 문자열
     */
    public static String tenantConfigResponse(
            String tenantId, boolean mfaRequired, List<String> allowedSocialLogins) {
        StringBuilder socialLoginsJson = new StringBuilder("[");
        for (int i = 0; i < allowedSocialLogins.size(); i++) {
            socialLoginsJson.append("\"").append(allowedSocialLogins.get(i)).append("\"");
            if (i < allowedSocialLogins.size() - 1) {
                socialLoginsJson.append(",");
            }
        }
        socialLoginsJson.append("]");

        return String.format(
                """
                {
                    "tenantId": "%s",
                    "mfaRequired": %s,
                    "allowedSocialLogins": %s,
                    "roleHierarchy": {
                        "ADMIN": ["USER", "VIEWER"],
                        "USER": ["VIEWER"]
                    },
                    "sessionConfig": {
                        "maxActiveSessions": 5,
                        "accessTokenTTLSeconds": 900,
                        "refreshTokenTTLSeconds": 604800
                    },
                    "rateLimitConfig": {
                        "loginAttemptsPerHour": 10,
                        "otpRequestsPerHour": 5
                    }
                }
                """,
                tenantId, mfaRequired, socialLoginsJson);
    }

    /** MFA 필수 Tenant ID */
    public static final String MFA_REQUIRED_TENANT = "tenant-mfa-001";

    /** MFA 불필요 Tenant ID */
    public static final String MFA_NOT_REQUIRED_TENANT = "tenant-001";

    /** 소셜 로그인 제한 Tenant ID */
    public static final String SOCIAL_RESTRICTED_TENANT = "tenant-social-001";
}
