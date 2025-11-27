package com.ryuqq.gateway.adapter.out.redis.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.Set;

/**
 * Tenant Config Entity (Plain Java, Lombok 금지)
 *
 * <p>Redis에 저장되는 Tenant Config Entity
 *
 * <p>JSON 직렬화/역직렬화 지원
 *
 * <p><strong>Redis Key</strong>: {@code gateway:tenant:config:{tenantId}}
 *
 * <p><strong>TTL</strong>: 1시간
 *
 * @author development-team
 * @since 1.0.0
 */
public final class TenantConfigEntity {

    private final String tenantId;
    private final boolean mfaRequired;
    private final Set<String> allowedSocialLogins;
    private final Map<String, Set<String>> roleHierarchy;
    private final SessionConfigEntity sessionConfig;
    private final TenantRateLimitConfigEntity rateLimitConfig;

    /** Constructor (Jackson 역직렬화용) */
    @JsonCreator
    public TenantConfigEntity(
            @JsonProperty("tenantId") String tenantId,
            @JsonProperty("mfaRequired") boolean mfaRequired,
            @JsonProperty("allowedSocialLogins") Set<String> allowedSocialLogins,
            @JsonProperty("roleHierarchy") Map<String, Set<String>> roleHierarchy,
            @JsonProperty("sessionConfig") SessionConfigEntity sessionConfig,
            @JsonProperty("rateLimitConfig") TenantRateLimitConfigEntity rateLimitConfig) {
        this.tenantId = tenantId;
        this.mfaRequired = mfaRequired;
        this.allowedSocialLogins =
                allowedSocialLogins != null ? Set.copyOf(allowedSocialLogins) : Set.of();
        this.roleHierarchy = roleHierarchy != null ? Map.copyOf(roleHierarchy) : Map.of();
        this.sessionConfig = sessionConfig;
        this.rateLimitConfig = rateLimitConfig;
    }

    // Getters (Jackson 직렬화용)
    public String getTenantId() {
        return tenantId;
    }

    public boolean isMfaRequired() {
        return mfaRequired;
    }

    public Set<String> getAllowedSocialLogins() {
        return allowedSocialLogins;
    }

    public Map<String, Set<String>> getRoleHierarchy() {
        return roleHierarchy;
    }

    public SessionConfigEntity getSessionConfig() {
        return sessionConfig;
    }

    public TenantRateLimitConfigEntity getRateLimitConfig() {
        return rateLimitConfig;
    }

    @Override
    public String toString() {
        return "TenantConfigEntity{"
                + "tenantId='"
                + tenantId
                + '\''
                + ", mfaRequired="
                + mfaRequired
                + ", allowedSocialLogins="
                + allowedSocialLogins
                + ", roleHierarchy="
                + roleHierarchy
                + ", sessionConfig="
                + sessionConfig
                + ", rateLimitConfig="
                + rateLimitConfig
                + '}';
    }
}
