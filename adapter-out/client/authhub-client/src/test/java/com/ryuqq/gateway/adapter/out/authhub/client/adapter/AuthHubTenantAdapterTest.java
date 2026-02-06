package com.ryuqq.gateway.adapter.out.authhub.client.adapter;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import com.ryuqq.gateway.adapter.out.authhub.client.config.AuthHubProperties;
import com.ryuqq.gateway.adapter.out.authhub.client.mapper.AuthHubTenantMapper;
import com.ryuqq.gateway.adapter.out.authhub.client.mapper.AuthHubTenantMapper.TenantConfigResponse;
import com.ryuqq.gateway.domain.tenant.aggregate.TenantConfig;
import com.ryuqq.gateway.domain.tenant.id.TenantId;
import com.ryuqq.gateway.domain.tenant.vo.SessionConfig;
import com.ryuqq.gateway.domain.tenant.vo.SocialProvider;
import com.ryuqq.gateway.domain.tenant.vo.TenantRateLimitConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * AuthHubTenantAdapter 단위 테스트
 *
 * <p>SDK의 TenantConfig 모델이 도메인 요구사항을 충족하지 못하므로 WebClient를 사용합니다.
 *
 * @author development-team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthHubTenantAdapter 단위 테스트")
class AuthHubTenantAdapterTest {

    @Mock private WebClient webClient;
    @Mock private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock private WebClient.RequestHeadersSpec requestHeadersSpec;
    @Mock private WebClient.ResponseSpec responseSpec;
    @Mock private AuthHubProperties properties;
    @Mock private AuthHubTenantMapper tenantMapper;

    private RetryRegistry retryRegistry;
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private AuthHubTenantAdapter adapter;

    @BeforeEach
    void setUp() {
        RetryConfig retryConfig =
                RetryConfig.custom().maxAttempts(1).waitDuration(Duration.ofMillis(10)).build();
        retryRegistry = RetryRegistry.of(retryConfig);

        CircuitBreakerConfig cbConfig =
                CircuitBreakerConfig.custom()
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofSeconds(1))
                        .permittedNumberOfCallsInHalfOpenState(1)
                        .slidingWindowSize(2)
                        .build();
        circuitBreakerRegistry = CircuitBreakerRegistry.of(cbConfig);

        adapter =
                new AuthHubTenantAdapter(
                        webClient, properties, tenantMapper, retryRegistry, circuitBreakerRegistry);
    }

    @Nested
    @DisplayName("fetchTenantConfig 테스트")
    class FetchTenantConfigTest {

        @Test
        @DisplayName("Tenant Config 조회 성공 (WebClient)")
        @SuppressWarnings("unchecked")
        void fetchTenantConfig_success() {
            // given
            String tenantId = "tenant-1";

            AuthHubTenantMapper.SessionConfigResponse sessionConfigResponse =
                    new AuthHubTenantMapper.SessionConfigResponse(3, 3600L, 86400L);
            AuthHubTenantMapper.RateLimitConfigResponse rateLimitConfigResponse =
                    new AuthHubTenantMapper.RateLimitConfigResponse(10, 5);
            TenantConfigResponse webResponse =
                    new TenantConfigResponse(
                            tenantId,
                            true,
                            List.of("GOOGLE", "NAVER"),
                            Map.of("ADMIN", Set.of("USER")),
                            sessionConfigResponse,
                            rateLimitConfigResponse);

            TenantConfig domainConfig =
                    TenantConfig.of(
                            TenantId.from(tenantId),
                            true,
                            Set.of(SocialProvider.GOOGLE, SocialProvider.NAVER),
                            Map.of("ADMIN", Set.of("USER")),
                            SessionConfig.ofSeconds(3, 3600L, 86400L),
                            TenantRateLimitConfig.of(10, 5));

            given(properties.getTenantConfigEndpoint())
                    .willReturn("/api/v1/tenants/{tenantId}/config");
            given(webClient.get()).willReturn(requestHeadersUriSpec);
            given(requestHeadersUriSpec.uri(anyString())).willReturn(requestHeadersSpec);
            given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
            given(responseSpec.bodyToMono(TenantConfigResponse.class))
                    .willReturn(Mono.just(webResponse));
            given(tenantMapper.toTenantConfig(webResponse)).willReturn(domainConfig);

            // when & then
            StepVerifier.create(adapter.fetchTenantConfig(tenantId))
                    .assertNext(
                            config -> {
                                assertThat(config.isMfaRequired()).isTrue();
                                assertThat(config.getTenantIdValue()).isEqualTo(tenantId);
                            })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Tenant Config 조회 실패 - WebClient 예외 (404)")
        @SuppressWarnings("unchecked")
        void fetchTenantConfig_notFound() {
            // given
            String tenantId = "non-existent-tenant";

            given(properties.getTenantConfigEndpoint())
                    .willReturn("/api/v1/tenants/{tenantId}/config");
            given(webClient.get()).willReturn(requestHeadersUriSpec);
            given(requestHeadersUriSpec.uri(anyString())).willReturn(requestHeadersSpec);
            given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
            given(responseSpec.bodyToMono(TenantConfigResponse.class))
                    .willReturn(
                            Mono.error(
                                    WebClientResponseException.create(
                                            404, "Not Found", null, null, null)));

            // when & then
            StepVerifier.create(adapter.fetchTenantConfig(tenantId))
                    .expectError(WebClientResponseException.class)
                    .verify();
        }

        @Test
        @DisplayName("Tenant Config 조회 실패 - WebClient 예외 (500)")
        @SuppressWarnings("unchecked")
        void fetchTenantConfig_serverError() {
            // given
            String tenantId = "tenant-1";

            given(properties.getTenantConfigEndpoint())
                    .willReturn("/api/v1/tenants/{tenantId}/config");
            given(webClient.get()).willReturn(requestHeadersUriSpec);
            given(requestHeadersUriSpec.uri(anyString())).willReturn(requestHeadersSpec);
            given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
            given(responseSpec.bodyToMono(TenantConfigResponse.class))
                    .willReturn(
                            Mono.error(
                                    WebClientResponseException.create(
                                            500, "Internal Server Error", null, null, null)));

            // when & then
            StepVerifier.create(adapter.fetchTenantConfig(tenantId))
                    .expectError(WebClientResponseException.class)
                    .verify();
        }

        @Test
        @DisplayName("Tenant Config 조회 실패 - Mapper 예외")
        @SuppressWarnings("unchecked")
        void fetchTenantConfig_mapperException() {
            // given
            String tenantId = "tenant-1";

            TenantConfigResponse webResponse =
                    new TenantConfigResponse(
                            tenantId,
                            true,
                            List.of("GOOGLE"),
                            Map.of(),
                            null, // null session config triggers default
                            null);

            TenantConfig domainConfig =
                    TenantConfig.of(
                            TenantId.from(tenantId),
                            true,
                            Set.of(SocialProvider.GOOGLE),
                            Map.of(),
                            SessionConfig.defaultConfig(),
                            TenantRateLimitConfig.defaultConfig());

            given(properties.getTenantConfigEndpoint())
                    .willReturn("/api/v1/tenants/{tenantId}/config");
            given(webClient.get()).willReturn(requestHeadersUriSpec);
            given(requestHeadersUriSpec.uri(anyString())).willReturn(requestHeadersSpec);
            given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
            given(responseSpec.bodyToMono(TenantConfigResponse.class))
                    .willReturn(Mono.just(webResponse));
            given(tenantMapper.toTenantConfig(webResponse)).willReturn(domainConfig);

            // when & then
            StepVerifier.create(adapter.fetchTenantConfig(tenantId))
                    .assertNext(
                            config -> {
                                assertThat(config.getSessionConfig()).isNotNull();
                            })
                    .verifyComplete();
        }
    }
}
