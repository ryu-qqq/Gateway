package com.ryuqq.gateway.adapter.out.authhub.client.mapper;

import com.ryuqq.gateway.adapter.out.authhub.client.exception.AuthHubClientException.TenantException;
import com.ryuqq.gateway.domain.tenant.aggregate.TenantConfig;
import com.ryuqq.gateway.domain.tenant.id.TenantId;
import com.ryuqq.gateway.domain.tenant.vo.SessionConfig;
import com.ryuqq.gateway.domain.tenant.vo.SocialProvider;
import com.ryuqq.gateway.domain.tenant.vo.TenantRateLimitConfig;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * AuthHub Tenant Mapper
 *
 * <p>Tenant 관련 응답을 Domain 객체로 변환
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class AuthHubTenantMapper {

    /**
     * Tenant Config Response → Domain Aggregate 변환
     *
     * @param response API 응답
     * @return TenantConfig
     */
    public TenantConfig toTenantConfig(TenantConfigResponse response) {
        if (response == null) {
            throw new TenantException("Empty Tenant Config response");
        }

        Set<SocialProvider> allowedSocialLogins =
                response.allowedSocialLogins().stream()
                        .map(SocialProvider::fromName)
                        .collect(Collectors.toSet());

        SessionConfig sessionConfig = toSessionConfig(response.sessionConfig());
        TenantRateLimitConfig rateLimitConfig = toRateLimitConfig(response.rateLimitConfig());

        return TenantConfig.of(
                TenantId.from(response.tenantId()),
                response.mfaRequired(),
                allowedSocialLogins,
                response.roleHierarchy(),
                sessionConfig,
                rateLimitConfig);
    }

    /**
     * Session Config Response → Domain VO 변환
     *
     * @param sessionConfigResponse Session Config 응답
     * @return SessionConfig
     */
    public SessionConfig toSessionConfig(SessionConfigResponse sessionConfigResponse) {
        if (sessionConfigResponse == null) {
            return SessionConfig.defaultConfig();
        }

        return SessionConfig.ofSeconds(
                sessionConfigResponse.maxActiveSessions(),
                sessionConfigResponse.accessTokenTTLSeconds(),
                sessionConfigResponse.refreshTokenTTLSeconds());
    }

    /**
     * Rate Limit Config Response → Domain VO 변환
     *
     * @param rateLimitConfigResponse Rate Limit Config 응답
     * @return TenantRateLimitConfig
     */
    public TenantRateLimitConfig toRateLimitConfig(
            RateLimitConfigResponse rateLimitConfigResponse) {
        if (rateLimitConfigResponse == null) {
            return TenantRateLimitConfig.defaultConfig();
        }

        return TenantRateLimitConfig.of(
                rateLimitConfigResponse.loginAttemptsPerHour(),
                rateLimitConfigResponse.otpRequestsPerHour());
    }

    /** Tenant Config Response DTO */
    public record TenantConfigResponse(
            String tenantId,
            boolean mfaRequired,
            List<String> allowedSocialLogins,
            Map<String, Set<String>> roleHierarchy,
            SessionConfigResponse sessionConfig,
            RateLimitConfigResponse rateLimitConfig) {}

    /** Session Config Response DTO */
    public record SessionConfigResponse(
            int maxActiveSessions, long accessTokenTTLSeconds, long refreshTokenTTLSeconds) {}

    /** Rate Limit Config Response DTO */
    public record RateLimitConfigResponse(int loginAttemptsPerHour, int otpRequestsPerHour) {}
}
