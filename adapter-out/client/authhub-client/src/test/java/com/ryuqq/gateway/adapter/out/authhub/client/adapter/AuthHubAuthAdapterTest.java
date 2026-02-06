package com.ryuqq.gateway.adapter.out.authhub.client.adapter;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import com.ryuqq.authhub.sdk.api.AuthApi;
import com.ryuqq.authhub.sdk.api.InternalApi;
import com.ryuqq.authhub.sdk.client.AuthHubClient;
import com.ryuqq.authhub.sdk.client.GatewayClient;
import com.ryuqq.authhub.sdk.exception.AuthHubBadRequestException;
import com.ryuqq.authhub.sdk.exception.AuthHubServerException;
import com.ryuqq.authhub.sdk.model.auth.RefreshTokenRequest;
import com.ryuqq.authhub.sdk.model.auth.TokenResponse;
import com.ryuqq.authhub.sdk.model.common.ApiResponse;
import com.ryuqq.authhub.sdk.model.internal.PublicKeys;
import com.ryuqq.gateway.adapter.out.authhub.client.exception.AuthHubClientException.AuthException;
import com.ryuqq.gateway.adapter.out.authhub.client.mapper.AuthHubAuthMapper;
import com.ryuqq.gateway.domain.authentication.vo.PublicKey;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

/**
 * AuthHubAuthAdapter 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthHubAuthAdapter 단위 테스트")
class AuthHubAuthAdapterTest {

    @Mock private AuthHubClient authHubSdkClient;
    @Mock private GatewayClient gatewayClient;
    @Mock private InternalApi internalApi;
    @Mock private AuthApi authApi;
    @Mock private AuthHubAuthMapper authMapper;

    private RetryRegistry retryRegistry;
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private AuthHubAuthAdapter adapter;

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
                new AuthHubAuthAdapter(
                        authHubSdkClient,
                        gatewayClient,
                        authMapper,
                        retryRegistry,
                        circuitBreakerRegistry);
    }

    @Nested
    @DisplayName("fetchPublicKeys 테스트")
    class FetchPublicKeysTest {

        @Test
        @DisplayName("JWKS 조회 성공 (SDK) - getJwks()는 PublicKeys를 직접 반환")
        void fetchPublicKeys_success() {
            // given
            // SDK PublicKey: kid, kty, use, alg, n, e
            com.ryuqq.authhub.sdk.model.internal.PublicKey sdkPublicKey =
                    new com.ryuqq.authhub.sdk.model.internal.PublicKey(
                            "kid-1", "RSA", "sig", "RS256", "n-value", "e-value");
            PublicKeys publicKeys = new PublicKeys(List.of(sdkPublicKey));

            // Domain PublicKey: kid, n, e, kty, use, alg
            PublicKey domainPublicKey =
                    new PublicKey("kid-1", "n-value", "e-value", "RSA", "sig", "RS256");

            // SDK의 getJwks()는 PublicKeys를 직접 반환 (ApiResponse가 아님)
            given(gatewayClient.internal()).willReturn(internalApi);
            given(internalApi.getJwks()).willReturn(publicKeys);
            given(authMapper.toPublicKeys(publicKeys)).willReturn(List.of(domainPublicKey));

            // when & then
            StepVerifier.create(adapter.fetchPublicKeys())
                    .expectNext(domainPublicKey)
                    .verifyComplete();
        }

        @Test
        @DisplayName("JWKS 조회 실패 - null 응답")
        void fetchPublicKeys_nullResponse() {
            // given
            given(gatewayClient.internal()).willReturn(internalApi);
            given(internalApi.getJwks()).willReturn(null);

            // when & then
            StepVerifier.create(adapter.fetchPublicKeys())
                    .expectError(AuthException.class)
                    .verify();
        }

        @Test
        @DisplayName("JWKS 조회 실패 - keys가 null")
        void fetchPublicKeys_nullKeys() {
            // given
            PublicKeys publicKeysWithNullKeys = new PublicKeys(null);

            given(gatewayClient.internal()).willReturn(internalApi);
            given(internalApi.getJwks()).willReturn(publicKeysWithNullKeys);

            // when & then
            StepVerifier.create(adapter.fetchPublicKeys())
                    .expectError(AuthException.class)
                    .verify();
        }

        @Test
        @DisplayName("JWKS 조회 실패 - SDK 예외")
        void fetchPublicKeys_sdkException() {
            // given
            given(gatewayClient.internal()).willReturn(internalApi);
            given(internalApi.getJwks())
                    .willThrow(
                            new AuthHubServerException(
                                    500, "Internal server error", "SERVER_ERROR"));

            // when & then
            StepVerifier.create(adapter.fetchPublicKeys())
                    .expectError(AuthException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("refreshAccessToken 테스트")
    class RefreshAccessTokenTest {

        // JWT 형식: header.payload.signature (header에 kid 클레임 포함)
        // Header: {"alg":"RS256","typ":"JWT","kid":"test-kid-1"}
        private static final String DUMMY_ACCESS_TOKEN =
                "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6InRlc3Qta2lkLTEifQ."
                    + "eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWV9."
                    + "EkN-DOsnsuRjRO6BxXemmJDm3HbxrbRzXglbN2S4sOkopdU4IsDxTI8jO19W_A4K8ZPJijNLis4EZsHeY559a4DFOd50_OqgHGuERTqYZyuhtF33th9ISSV";
        private static final String DUMMY_REFRESH_TOKEN =
                "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6InRlc3Qta2lkLTEifQ."
                    + "eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwidHlwZSI6InJlZnJlc2gifQ."
                    + "EkN-DOsnsuRjRO6BxXemmJDm3HbxrbRzXglbN2S4sOkopdU4IsDxTI8jO19W_A4K8ZPJijNLis4EZsHeY559a4DFOd50_REFRESH";

        @Test
        @DisplayName("토큰 갱신 성공 (SDK)")
        void refreshAccessToken_success() {
            // given
            String tenantId = "tenant-1";
            String refreshToken = DUMMY_REFRESH_TOKEN;
            String newAccessToken = DUMMY_ACCESS_TOKEN;
            String newRefreshToken = DUMMY_REFRESH_TOKEN;

            TokenResponse tokenResponse =
                    new TokenResponse(newAccessToken, newRefreshToken, 3600, 86400, "Bearer");
            ApiResponse<TokenResponse> apiResponse =
                    new ApiResponse<>(true, tokenResponse, LocalDateTime.now(), "req-123");

            given(authHubSdkClient.auth()).willReturn(authApi);
            given(authApi.refresh(any(RefreshTokenRequest.class))).willReturn(apiResponse);

            // when & then
            StepVerifier.create(adapter.refreshAccessToken(tenantId, refreshToken))
                    .assertNext(
                            pair -> {
                                assertThat(pair.accessTokenValue()).isEqualTo(newAccessToken);
                                assertThat(pair.refreshTokenValue()).isEqualTo(newRefreshToken);
                            })
                    .verifyComplete();
        }

        @Test
        @DisplayName("토큰 갱신 실패 - SDK 예외")
        void refreshAccessToken_sdkException() {
            // given
            String tenantId = "tenant-1";
            String refreshToken = "invalid-token";

            given(authHubSdkClient.auth()).willReturn(authApi);
            given(authApi.refresh(any(RefreshTokenRequest.class)))
                    .willThrow(
                            new AuthHubBadRequestException(
                                    "Invalid refresh token", "INVALID_TOKEN"));

            // when & then
            StepVerifier.create(adapter.refreshAccessToken(tenantId, refreshToken))
                    .expectError(AuthException.class)
                    .verify();
        }

        @Test
        @DisplayName("토큰 갱신 실패 - 빈 응답")
        void refreshAccessToken_emptyResponse() {
            // given
            String tenantId = "tenant-1";
            String refreshToken = "refresh-token";

            ApiResponse<TokenResponse> emptyResponse =
                    new ApiResponse<>(true, null, LocalDateTime.now(), "req-123");

            given(authHubSdkClient.auth()).willReturn(authApi);
            given(authApi.refresh(any(RefreshTokenRequest.class))).willReturn(emptyResponse);

            // when & then
            StepVerifier.create(adapter.refreshAccessToken(tenantId, refreshToken))
                    .expectError(AuthException.class)
                    .verify();
        }

        @Test
        @DisplayName("토큰 갱신 실패 - success=false")
        void refreshAccessToken_failureResponse() {
            // given
            String tenantId = "tenant-1";
            String refreshToken = "refresh-token";

            ApiResponse<TokenResponse> failureResponse =
                    new ApiResponse<>(false, null, LocalDateTime.now(), "req-123");

            given(authHubSdkClient.auth()).willReturn(authApi);
            given(authApi.refresh(any(RefreshTokenRequest.class))).willReturn(failureResponse);

            // when & then
            StepVerifier.create(adapter.refreshAccessToken(tenantId, refreshToken))
                    .expectError(AuthException.class)
                    .verify();
        }
    }
}
