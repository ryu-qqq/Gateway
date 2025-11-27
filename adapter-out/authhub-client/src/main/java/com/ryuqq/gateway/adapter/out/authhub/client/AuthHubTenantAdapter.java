package com.ryuqq.gateway.adapter.out.authhub.client;

import com.ryuqq.gateway.application.tenant.port.out.client.AuthHubTenantClient;
import com.ryuqq.gateway.domain.tenant.TenantConfig;
import com.ryuqq.gateway.domain.tenant.vo.SessionConfig;
import com.ryuqq.gateway.domain.tenant.vo.SocialProvider;
import com.ryuqq.gateway.domain.tenant.vo.TenantId;
import com.ryuqq.gateway.domain.tenant.vo.TenantRateLimitConfig;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * AuthHub Tenant Adapter
 *
 * <p>AuthHubTenantClient 구현체 (WebClient + Resilience4j)
 *
 * <p><strong>통신 대상</strong>:
 *
 * <ul>
 *   <li>AuthHub 외부 시스템
 *   <li>엔드포인트: {@code GET /api/v1/tenants/{tenantId}/config}
 * </ul>
 *
 * <p><strong>Resilience 전략</strong>:
 *
 * <ul>
 *   <li>Retry: 최대 3회 (Exponential Backoff)
 *   <li>Circuit Breaker: 50% 실패율 시 Open
 *   <li>Timeout: Connection 3초, Response 3초
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class AuthHubTenantAdapter implements AuthHubTenantClient {

    private static final String TENANT_CONFIG_ENDPOINT = "/api/v1/tenants/{tenantId}/config";

    private final WebClient webClient;

    public AuthHubTenantAdapter(@Qualifier("authHubWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Tenant Config 조회 (AuthHub API)
     *
     * @param tenantId 테넌트 ID
     * @return TenantConfig Domain Aggregate
     */
    @Override
    @Retry(name = "authHub", fallbackMethod = "fetchTenantConfigFallback")
    @CircuitBreaker(name = "authHub", fallbackMethod = "fetchTenantConfigFallback")
    public Mono<TenantConfig> fetchTenantConfig(String tenantId) {
        return webClient
                .get()
                .uri(TENANT_CONFIG_ENDPOINT, tenantId)
                .retrieve()
                .bodyToMono(TenantConfigResponse.class)
                .map(this::toTenantConfig)
                .onErrorMap(
                        e -> !(e instanceof AuthHubTenantException),
                        e -> new AuthHubTenantException(
                                "Failed to fetch Tenant Config: " + tenantId, e));
    }

    /**
     * Tenant Config 응답 → Domain Aggregate 변환
     *
     * @param response API 응답
     * @return TenantConfig
     */
    TenantConfig toTenantConfig(TenantConfigResponse response) {
        if (response == null) {
            throw new AuthHubTenantException("Empty Tenant Config response");
        }

        Set<SocialProvider> allowedSocialLogins = response.allowedSocialLogins().stream()
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
     * Session Config 응답 → Domain VO 변환
     *
     * @param sessionConfigResponse Session Config 응답
     * @return SessionConfig
     */
    SessionConfig toSessionConfig(SessionConfigResponse sessionConfigResponse) {
        if (sessionConfigResponse == null) {
            return SessionConfig.defaultConfig();
        }

        return SessionConfig.ofSeconds(
                sessionConfigResponse.maxActiveSessions(),
                sessionConfigResponse.accessTokenTTLSeconds(),
                sessionConfigResponse.refreshTokenTTLSeconds());
    }

    /**
     * Rate Limit Config 응답 → Domain VO 변환
     *
     * @param rateLimitConfigResponse Rate Limit Config 응답
     * @return TenantRateLimitConfig
     */
    TenantRateLimitConfig toRateLimitConfig(RateLimitConfigResponse rateLimitConfigResponse) {
        if (rateLimitConfigResponse == null) {
            return TenantRateLimitConfig.defaultConfig();
        }

        return TenantRateLimitConfig.of(
                rateLimitConfigResponse.loginAttemptsPerHour(),
                rateLimitConfigResponse.otpRequestsPerHour());
    }

    /**
     * Fallback 메서드 (Retry/Circuit Breaker 실패 시)
     *
     * @param tenantId 테넌트 ID
     * @param throwable 예외
     * @return Mono.error
     */
    @SuppressWarnings("unused")
    Mono<TenantConfig> fetchTenantConfigFallback(String tenantId, Throwable throwable) {
        return Mono.error(
                new AuthHubTenantException(
                        "Tenant Config 조회 실패 (Fallback): " + tenantId, throwable));
    }

    /** Tenant Config Response DTO */
    record TenantConfigResponse(
            String tenantId,
            boolean mfaRequired,
            List<String> allowedSocialLogins,
            Map<String, Set<String>> roleHierarchy,
            SessionConfigResponse sessionConfig,
            RateLimitConfigResponse rateLimitConfig) {}

    /** Session Config Response DTO */
    record SessionConfigResponse(
            int maxActiveSessions,
            long accessTokenTTLSeconds,
            long refreshTokenTTLSeconds) {}

    /** Rate Limit Config Response DTO */
    record RateLimitConfigResponse(
            int loginAttemptsPerHour,
            int otpRequestsPerHour) {}

    /** AuthHub Tenant 예외 */
    public static class AuthHubTenantException extends RuntimeException {
        public AuthHubTenantException(String message) {
            super(message);
        }

        public AuthHubTenantException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
