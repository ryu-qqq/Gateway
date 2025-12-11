package com.ryuqq.gateway.adapter.out.redis.mapper;

import com.ryuqq.gateway.adapter.out.redis.entity.SessionConfigEntity;
import com.ryuqq.gateway.adapter.out.redis.entity.TenantConfigEntity;
import com.ryuqq.gateway.adapter.out.redis.entity.TenantRateLimitConfigEntity;
import com.ryuqq.gateway.domain.tenant.TenantConfig;
import com.ryuqq.gateway.domain.tenant.vo.SessionConfig;
import com.ryuqq.gateway.domain.tenant.vo.SocialProvider;
import com.ryuqq.gateway.domain.tenant.vo.TenantId;
import com.ryuqq.gateway.domain.tenant.vo.TenantRateLimitConfig;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Tenant Config Mapper
 *
 * <p>TenantConfig (Domain Aggregate) ↔ TenantConfigEntity (Redis Entity) 양방향 매핑
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>Domain Aggregate → Redis Entity 변환
 *   <li>Redis Entity → Domain Aggregate 변환
 *   <li>내부 VO/Entity 간 변환 포함
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class TenantConfigMapper {

    /**
     * Redis Entity → Domain Aggregate
     *
     * @param entity TenantConfigEntity
     * @return TenantConfig
     */
    public TenantConfig toTenantConfig(TenantConfigEntity entity) {
        if (entity == null) {
            return null;
        }

        Set<SocialProvider> allowedSocialLogins =
                entity.getAllowedSocialLogins().stream()
                        .map(SocialProvider::fromName)
                        .collect(Collectors.toSet());

        SessionConfig sessionConfig = toSessionConfig(entity.getSessionConfig());
        TenantRateLimitConfig rateLimitConfig = toRateLimitConfig(entity.getRateLimitConfig());

        return TenantConfig.of(
                TenantId.from(entity.getTenantId()),
                entity.isMfaRequired(),
                allowedSocialLogins,
                entity.getRoleHierarchy(),
                sessionConfig,
                rateLimitConfig);
    }

    /**
     * Domain Aggregate → Redis Entity
     *
     * @param tenantConfig TenantConfig
     * @return TenantConfigEntity
     */
    public TenantConfigEntity toTenantConfigEntity(TenantConfig tenantConfig) {
        if (tenantConfig == null) {
            return null;
        }

        Set<String> allowedSocialLogins =
                tenantConfig.getAllowedSocialLogins().stream()
                        .map(SocialProvider::name)
                        .collect(Collectors.toSet());

        SessionConfigEntity sessionConfigEntity =
                toSessionConfigEntity(tenantConfig.getSessionConfig());
        TenantRateLimitConfigEntity rateLimitConfigEntity =
                toRateLimitConfigEntity(tenantConfig.getRateLimitConfig());

        return new TenantConfigEntity(
                tenantConfig.getTenantIdValue(),
                tenantConfig.isMfaRequired(),
                allowedSocialLogins,
                tenantConfig.getRoleHierarchy(),
                sessionConfigEntity,
                rateLimitConfigEntity);
    }

    /** SessionConfigEntity → SessionConfig */
    private SessionConfig toSessionConfig(SessionConfigEntity entity) {
        if (entity == null) {
            return SessionConfig.defaultConfig();
        }

        return SessionConfig.ofSeconds(
                entity.getMaxActiveSessions(),
                entity.getAccessTokenTTLSeconds(),
                entity.getRefreshTokenTTLSeconds());
    }

    /** SessionConfig → SessionConfigEntity */
    private SessionConfigEntity toSessionConfigEntity(SessionConfig sessionConfig) {
        if (sessionConfig == null) {
            return null;
        }

        return new SessionConfigEntity(
                sessionConfig.maxActiveSessions(),
                sessionConfig.accessTokenTTLSeconds(),
                sessionConfig.refreshTokenTTLSeconds());
    }

    /** TenantRateLimitConfigEntity → TenantRateLimitConfig */
    private TenantRateLimitConfig toRateLimitConfig(TenantRateLimitConfigEntity entity) {
        if (entity == null) {
            return TenantRateLimitConfig.defaultConfig();
        }

        return TenantRateLimitConfig.of(
                entity.getLoginAttemptsPerHour(), entity.getOtpRequestsPerHour());
    }

    /** TenantRateLimitConfig → TenantRateLimitConfigEntity */
    private TenantRateLimitConfigEntity toRateLimitConfigEntity(
            TenantRateLimitConfig rateLimitConfig) {
        if (rateLimitConfig == null) {
            return null;
        }

        return new TenantRateLimitConfigEntity(
                rateLimitConfig.loginAttemptsPerHour(), rateLimitConfig.otpRequestsPerHour());
    }
}
