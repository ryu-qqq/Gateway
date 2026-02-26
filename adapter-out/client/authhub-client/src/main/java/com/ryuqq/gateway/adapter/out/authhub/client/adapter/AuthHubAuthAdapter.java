package com.ryuqq.gateway.adapter.out.authhub.client.adapter;

import com.ryuqq.authhub.sdk.client.AuthHubClient;
import com.ryuqq.authhub.sdk.client.GatewayClient;
import com.ryuqq.authhub.sdk.exception.AuthHubException;
import com.ryuqq.authhub.sdk.model.auth.RefreshTokenRequest;
import com.ryuqq.authhub.sdk.model.auth.TokenResponse;
import com.ryuqq.authhub.sdk.model.common.ApiResponse;
import com.ryuqq.authhub.sdk.model.internal.PublicKeys;
import com.ryuqq.gateway.adapter.out.authhub.client.exception.AuthHubClientException.AuthException;
import com.ryuqq.gateway.adapter.out.authhub.client.mapper.AuthHubAuthMapper;
import com.ryuqq.gateway.domain.authentication.vo.PublicKey;
import com.ryuqq.gateway.domain.authentication.vo.TokenPair;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * AuthHub Auth Adapter
 *
 * <p>인증 관련 AuthHub API 호출 Adapter
 *
 * <p><strong>구현 방식</strong>:
 *
 * <ul>
 *   <li>JWKS: SDK (GatewayClient.internal().getJwks())
 *   <li>Token Refresh: SDK (AuthHubClient.auth().refresh())
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
public class AuthHubAuthAdapter
        implements com.ryuqq.gateway.application.authentication.port.out.client.AuthHubClient {

    private static final Logger log = LoggerFactory.getLogger(AuthHubAuthAdapter.class);
    private static final String CIRCUIT_BREAKER_NAME = "authHubAuth";

    private final AuthHubClient authHubSdkClient;
    private final GatewayClient gatewayClient;
    private final AuthHubAuthMapper authMapper;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;

    public AuthHubAuthAdapter(
            AuthHubClient authHubClient,
            GatewayClient gatewayClient,
            AuthHubAuthMapper authMapper,
            RetryRegistry retryRegistry,
            CircuitBreakerRegistry circuitBreakerRegistry) {
        this.authHubSdkClient = authHubClient;
        this.gatewayClient = gatewayClient;
        this.authMapper = authMapper;
        this.retry = retryRegistry.retry(CIRCUIT_BREAKER_NAME);
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_NAME);
    }

    /**
     * JWKS 엔드포인트 호출 (SDK)
     *
     * <p>GatewayClient SDK를 사용하여 JWKS를 조회합니다.
     *
     * @return Flux&lt;PublicKey&gt; Public Key 스트림
     */
    @Override
    public Flux<PublicKey> fetchPublicKeys() {
        log.debug("Fetching JWKS from AuthHub via SDK");

        return Mono.<java.util.List<PublicKey>>fromCallable(
                        () -> {
                            try {
                                // SDK의 getJwks()는 PublicKeys를 직접 반환 (ApiResponse 아님)
                                PublicKeys publicKeys = gatewayClient.internal().getJwks();

                                if (publicKeys == null || publicKeys.keys() == null) {
                                    throw new AuthException(
                                            "Failed to fetch JWKS: empty response from AuthHub");
                                }

                                return authMapper.toPublicKeys(publicKeys);
                            } catch (AuthHubException e) {
                                throw new AuthException(
                                        "Failed to fetch JWKS: " + e.getMessage(), e);
                            }
                        })
                .flatMapMany(Flux::fromIterable)
                .transformDeferred(RetryOperator.of(retry))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .doOnNext(key -> log.debug("Fetched public key: kid={}", key.kid()))
                .doOnError(e -> log.error("Failed to fetch JWKS from AuthHub", e));
    }

    /**
     * Access Token Refresh 호출 (SDK)
     *
     * <p>AuthHub SDK를 사용하여 Token을 갱신합니다. tenantId는 refresh token의 JWT 클레임에 포함되어 있습니다.
     *
     * @param tenantId Tenant 식별자 (로깅용, JWT에서 자동 추출)
     * @param refreshToken 현재 유효한 Refresh Token
     * @return Mono&lt;TokenPair&gt; 새 Access Token + Refresh Token
     */
    @Override
    public Mono<TokenPair> refreshAccessToken(String tenantId, String refreshToken) {
        log.debug("Refreshing access token via SDK for tenant: {}", tenantId);

        return Mono.<TokenPair>fromCallable(
                        () -> {
                            try {
                                RefreshTokenRequest request = new RefreshTokenRequest(refreshToken);
                                ApiResponse<TokenResponse> response =
                                        authHubSdkClient.auth().refresh(request);

                                if (!response.success() || response.data() == null) {
                                    throw new AuthException(
                                            "Token refresh failed: empty response from AuthHub");
                                }

                                TokenResponse tokenResponse = response.data();
                                return TokenPair.of(
                                        tokenResponse.accessToken(), tokenResponse.refreshToken());
                            } catch (AuthHubException e) {
                                throw new AuthException(
                                        "Token refresh failed: " + e.getMessage(), e);
                            }
                        })
                .transformDeferred(RetryOperator.of(retry))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .doOnSuccess(
                        pair -> log.debug("Token refreshed successfully for tenant: {}", tenantId))
                .doOnError(e -> log.error("Failed to refresh token for tenant: {}", tenantId, e));
    }
}
