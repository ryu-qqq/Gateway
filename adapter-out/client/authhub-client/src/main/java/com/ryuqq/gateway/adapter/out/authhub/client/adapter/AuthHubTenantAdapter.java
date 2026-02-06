package com.ryuqq.gateway.adapter.out.authhub.client.adapter;

import com.ryuqq.gateway.adapter.out.authhub.client.config.AuthHubProperties;
import com.ryuqq.gateway.adapter.out.authhub.client.mapper.AuthHubTenantMapper;
import com.ryuqq.gateway.adapter.out.authhub.client.mapper.AuthHubTenantMapper.TenantConfigResponse;
import com.ryuqq.gateway.application.tenant.port.out.client.AuthClient;
import com.ryuqq.gateway.domain.tenant.aggregate.TenantConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * AuthHub Tenant Adapter
 *
 * <p>Tenant 관련 AuthHub API 호출 Adapter
 *
 * <p><strong>구현 방식</strong>:
 *
 * <ul>
 *   <li>Tenant Config: WebClient (SDK 모델이 도메인 요구사항과 맞지 않음)
 * </ul>
 *
 * <p><strong>Resilience 전략</strong>:
 *
 * <ul>
 *   <li>Retry: 최대 3회 (Exponential Backoff)
 *   <li>Circuit Breaker: 50% 실패율 시 Open
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class AuthHubTenantAdapter implements AuthClient {

    private static final Logger log = LoggerFactory.getLogger(AuthHubTenantAdapter.class);
    private static final String CIRCUIT_BREAKER_NAME = "authHubTenant";

    private final WebClient webClient;
    private final AuthHubProperties properties;
    private final AuthHubTenantMapper tenantMapper;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;

    public AuthHubTenantAdapter(
            WebClient authHubWebClient,
            AuthHubProperties properties,
            AuthHubTenantMapper tenantMapper,
            RetryRegistry retryRegistry,
            CircuitBreakerRegistry circuitBreakerRegistry) {
        this.webClient = authHubWebClient;
        this.properties = properties;
        this.tenantMapper = tenantMapper;
        this.retry = retryRegistry.retry(CIRCUIT_BREAKER_NAME);
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_NAME);
    }

    /**
     * AuthHub API에서 Tenant Config 조회 (WebClient)
     *
     * <p>SDK의 TenantConfig 모델이 도메인 요구사항(mfaRequired, allowedSocialLogins, sessionConfig 등)을 충족하지
     * 못하므로 WebClient를 사용합니다.
     *
     * <p>엔드포인트: {@code GET /api/v1/tenants/{tenantId}/config}
     *
     * @param tenantId 테넌트 ID
     * @return Mono&lt;TenantConfig&gt;
     */
    @Override
    public Mono<TenantConfig> fetchTenantConfig(String tenantId) {
        log.debug("Fetching tenant config from AuthHub: tenantId={}", tenantId);

        String endpoint = properties.getTenantConfigEndpoint().replace("{tenantId}", tenantId);

        return webClient
                .get()
                .uri(endpoint)
                .retrieve()
                .bodyToMono(TenantConfigResponse.class)
                .map(tenantMapper::toTenantConfig)
                .transformDeferred(RetryOperator.of(retry))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .doOnSuccess(
                        config ->
                                log.debug(
                                        "Fetched tenant config: tenantId={}, mfaRequired={}",
                                        tenantId,
                                        config.isMfaRequired()))
                .doOnError(
                        e -> log.error("Failed to fetch tenant config: tenantId={}", tenantId, e));
    }
}
