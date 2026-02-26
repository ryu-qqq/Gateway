package com.ryuqq.gateway.fixture.tenant;

import com.ryuqq.gateway.domain.tenant.aggregate.TenantConfig;
import com.ryuqq.gateway.domain.tenant.id.TenantId;

/**
 * TenantConfig 테스트용 Fixture (Object Mother Pattern)
 *
 * <p>테스트 데이터 생성을 중앙화하고 재사용성을 높임
 *
 * @author development-team
 * @since 1.0.0
 */
public final class TenantConfigFixture {

    private static final String DEFAULT_TENANT_ID = "test-tenant-1";

    private TenantConfigFixture() {}

    /**
     * 기본 TenantConfig 생성 (MFA 비활성화)
     *
     * @return TenantConfig
     */
    public static TenantConfig aDefaultTenantConfig() {
        return TenantConfig.of(TenantId.of(DEFAULT_TENANT_ID), false);
    }

    /**
     * 지정된 tenantId로 TenantConfig 생성 (MFA 비활성화)
     *
     * @param tenantId 테넌트 ID
     * @return TenantConfig
     */
    public static TenantConfig aTenantConfig(String tenantId) {
        return TenantConfig.of(TenantId.of(tenantId), false);
    }

    /**
     * MFA 활성화된 TenantConfig 생성
     *
     * @param tenantId 테넌트 ID
     * @return TenantConfig
     */
    public static TenantConfig aTenantConfigWithMfa(String tenantId) {
        return TenantConfig.of(TenantId.of(tenantId), true);
    }

    /**
     * 커스텀 MFA 설정으로 TenantConfig 생성
     *
     * @param tenantId 테넌트 ID
     * @param mfaRequired MFA 필수 여부
     * @return TenantConfig
     */
    public static TenantConfig aTenantConfig(String tenantId, boolean mfaRequired) {
        return TenantConfig.of(TenantId.of(tenantId), mfaRequired);
    }
}
