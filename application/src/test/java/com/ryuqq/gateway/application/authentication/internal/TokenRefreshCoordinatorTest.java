package com.ryuqq.gateway.application.authentication.internal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import com.ryuqq.gateway.application.authentication.dto.response.RefreshAccessTokenResponse;
import com.ryuqq.gateway.application.authentication.manager.RedisLockCommandManager;
import com.ryuqq.gateway.application.authentication.manager.RefreshTokenBlacklistCommandManager;
import com.ryuqq.gateway.application.authentication.manager.RefreshTokenBlacklistQueryManager;
import com.ryuqq.gateway.application.authentication.port.out.client.AuthHubClient;
import com.ryuqq.gateway.domain.authentication.exception.RefreshTokenReusedException;
import com.ryuqq.gateway.domain.authentication.exception.TokenRefreshFailedException;
import com.ryuqq.gateway.domain.authentication.vo.RefreshToken;
import com.ryuqq.gateway.domain.authentication.vo.TokenPair;
import com.ryuqq.gateway.fixture.authentication.AuthenticationFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * TokenRefreshCoordinator 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TokenRefreshCoordinator 단위 테스트")
class TokenRefreshCoordinatorTest {

    @Mock private RedisLockCommandManager redisLockCommandManager;

    @Mock private RefreshTokenBlacklistQueryManager blacklistQueryManager;

    @Mock private RefreshTokenBlacklistCommandManager blacklistCommandManager;

    @Mock private AuthHubClient authHubClient;

    @InjectMocks private TokenRefreshCoordinator tokenRefreshCoordinator;

    private static final String TENANT_ID = AuthenticationFixture.DEFAULT_TENANT_ID;
    private static final Long USER_ID = AuthenticationFixture.DEFAULT_USER_ID;

    @Nested
    @DisplayName("Lock 처리")
    class LockHandlingTest {

        @Test
        @DisplayName("Lock 획득 실패 시 TokenRefreshFailedException 발생")
        void shouldThrowExceptionWhenLockAcquisitionFails() {
            // given
            RefreshToken refreshToken = AuthenticationFixture.aRefreshToken();
            given(redisLockCommandManager.tryLock(TENANT_ID, USER_ID)).willReturn(Mono.just(false));

            // when
            Mono<RefreshAccessTokenResponse> result =
                    tokenRefreshCoordinator.coordinate(TENANT_ID, USER_ID, refreshToken);

            // then
            StepVerifier.create(result)
                    .expectErrorSatisfies(
                            error -> {
                                assertThat(error).isInstanceOf(TokenRefreshFailedException.class);
                                assertThat(error.getMessage()).contains("Failed to acquire lock");
                            })
                    .verify();

            then(blacklistQueryManager).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("Lock 획득 성공 후 정상 처리")
        void shouldProcessSuccessfullyAfterLockAcquisition() {
            // given
            RefreshToken refreshToken = AuthenticationFixture.aRefreshToken();
            TokenPair newTokenPair = AuthenticationFixture.aTokenPair();

            given(redisLockCommandManager.tryLock(TENANT_ID, USER_ID)).willReturn(Mono.just(true));
            given(redisLockCommandManager.unlock(TENANT_ID, USER_ID)).willReturn(Mono.empty());
            given(blacklistQueryManager.isBlacklisted(TENANT_ID, refreshToken))
                    .willReturn(Mono.just(false));
            given(authHubClient.refreshAccessToken(TENANT_ID, refreshToken.value()))
                    .willReturn(Mono.just(newTokenPair));
            given(
                            blacklistCommandManager.addToBlacklist(
                                    eq(TENANT_ID), eq(refreshToken), anyLong()))
                    .willReturn(Mono.empty());

            // when
            Mono<RefreshAccessTokenResponse> result =
                    tokenRefreshCoordinator.coordinate(TENANT_ID, USER_ID, refreshToken);

            // then
            StepVerifier.create(result)
                    .assertNext(
                            response -> {
                                assertThat(response.accessTokenValue())
                                        .isEqualTo(newTokenPair.accessTokenValue());
                                assertThat(response.refreshTokenValue())
                                        .isEqualTo(newTokenPair.refreshTokenValue());
                            })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Blacklist 처리")
    class BlacklistHandlingTest {

        @Test
        @DisplayName("Blacklist에 있는 토큰이면 RefreshTokenReusedException 발생")
        void shouldThrowExceptionWhenTokenIsBlacklisted() {
            // given
            RefreshToken refreshToken = AuthenticationFixture.aRefreshToken();

            given(redisLockCommandManager.tryLock(TENANT_ID, USER_ID)).willReturn(Mono.just(true));
            given(redisLockCommandManager.unlock(TENANT_ID, USER_ID)).willReturn(Mono.empty());
            given(blacklistQueryManager.isBlacklisted(TENANT_ID, refreshToken))
                    .willReturn(Mono.just(true));

            // when
            Mono<RefreshAccessTokenResponse> result =
                    tokenRefreshCoordinator.coordinate(TENANT_ID, USER_ID, refreshToken);

            // then
            StepVerifier.create(result)
                    .expectErrorSatisfies(
                            error -> {
                                assertThat(error).isInstanceOf(RefreshTokenReusedException.class);
                                assertThat(error.getMessage()).contains("reuse detected");
                            })
                    .verify();

            then(authHubClient).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("Token Rotation")
    class TokenRotationTest {

        @Test
        @DisplayName("새 토큰 발급 후 기존 토큰을 Blacklist에 추가해야 한다")
        void shouldAddOldTokenToBlacklistAfterRefresh() {
            // given
            RefreshToken refreshToken = AuthenticationFixture.aRefreshToken();
            TokenPair newTokenPair = AuthenticationFixture.aTokenPair();

            given(redisLockCommandManager.tryLock(TENANT_ID, USER_ID)).willReturn(Mono.just(true));
            given(redisLockCommandManager.unlock(TENANT_ID, USER_ID)).willReturn(Mono.empty());
            given(blacklistQueryManager.isBlacklisted(TENANT_ID, refreshToken))
                    .willReturn(Mono.just(false));
            given(authHubClient.refreshAccessToken(TENANT_ID, refreshToken.value()))
                    .willReturn(Mono.just(newTokenPair));
            given(
                            blacklistCommandManager.addToBlacklist(
                                    eq(TENANT_ID), eq(refreshToken), anyLong()))
                    .willReturn(Mono.empty());

            // when
            Mono<RefreshAccessTokenResponse> result =
                    tokenRefreshCoordinator.coordinate(TENANT_ID, USER_ID, refreshToken);

            // then
            StepVerifier.create(result).expectNextCount(1).verifyComplete();

            then(blacklistCommandManager)
                    .should()
                    .addToBlacklist(eq(TENANT_ID), eq(refreshToken), anyLong());
        }
    }

    @Nested
    @DisplayName("에러 처리")
    class ErrorHandlingTest {

        @Test
        @DisplayName("AuthHub 호출 실패 시 TokenRefreshFailedException으로 래핑")
        void shouldWrapAuthHubExceptionInTokenRefreshFailedException() {
            // given
            RefreshToken refreshToken = AuthenticationFixture.aRefreshToken();

            given(redisLockCommandManager.tryLock(TENANT_ID, USER_ID)).willReturn(Mono.just(true));
            given(redisLockCommandManager.unlock(TENANT_ID, USER_ID)).willReturn(Mono.empty());
            given(blacklistQueryManager.isBlacklisted(TENANT_ID, refreshToken))
                    .willReturn(Mono.just(false));
            given(authHubClient.refreshAccessToken(TENANT_ID, refreshToken.value()))
                    .willReturn(Mono.error(new RuntimeException("Network error")));

            // when
            Mono<RefreshAccessTokenResponse> result =
                    tokenRefreshCoordinator.coordinate(TENANT_ID, USER_ID, refreshToken);

            // then
            StepVerifier.create(result)
                    .expectErrorSatisfies(
                            error -> {
                                assertThat(error).isInstanceOf(TokenRefreshFailedException.class);
                                assertThat(error.getMessage()).contains("Token refresh failed");
                            })
                    .verify();
        }
    }
}
