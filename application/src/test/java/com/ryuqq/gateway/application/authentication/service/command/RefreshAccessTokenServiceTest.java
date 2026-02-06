package com.ryuqq.gateway.application.authentication.service.command;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import com.ryuqq.gateway.application.authentication.dto.command.RefreshAccessTokenCommand;
import com.ryuqq.gateway.application.authentication.dto.response.RefreshAccessTokenResponse;
import com.ryuqq.gateway.application.authentication.factory.AuthenticationFactory;
import com.ryuqq.gateway.application.authentication.internal.TokenRefreshCoordinator;
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
 * RefreshAccessTokenService 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshAccessTokenService 단위 테스트")
class RefreshAccessTokenServiceTest {

    @Mock private TokenRefreshCoordinator tokenRefreshCoordinator;

    @Mock private AuthenticationFactory authenticationFactory;

    @InjectMocks private RefreshAccessTokenService refreshAccessTokenService;

    @Nested
    @DisplayName("Token Refresh 실행")
    class ExecuteTest {

        @Test
        @DisplayName("정상적으로 Token을 Refresh해야 한다")
        void shouldRefreshTokenSuccessfully() {
            // given
            RefreshAccessTokenCommand command = AuthenticationFixture.aRefreshAccessTokenCommand();
            RefreshToken refreshToken = AuthenticationFixture.aRefreshToken();
            TokenPair tokenPair = AuthenticationFixture.aTokenPair();
            RefreshAccessTokenResponse expectedResponse =
                    RefreshAccessTokenResponse.from(tokenPair);

            given(authenticationFactory.createRefreshToken(command)).willReturn(refreshToken);
            given(
                            tokenRefreshCoordinator.coordinate(
                                    command.tenantId(), command.userId(), refreshToken))
                    .willReturn(Mono.just(expectedResponse));

            // when
            Mono<RefreshAccessTokenResponse> result = refreshAccessTokenService.execute(command);

            // then
            StepVerifier.create(result)
                    .assertNext(
                            response -> {
                                assertThat(response.accessTokenValue())
                                        .isEqualTo(tokenPair.accessTokenValue());
                                assertThat(response.refreshTokenValue())
                                        .isEqualTo(tokenPair.refreshTokenValue());
                            })
                    .verifyComplete();

            then(authenticationFactory).should().createRefreshToken(command);
            then(tokenRefreshCoordinator)
                    .should()
                    .coordinate(command.tenantId(), command.userId(), refreshToken);
        }

        @Test
        @DisplayName("Coordinator 실패 시 에러가 전파되어야 한다")
        void shouldPropagateErrorWhenCoordinatorFails() {
            // given
            RefreshAccessTokenCommand command = AuthenticationFixture.aRefreshAccessTokenCommand();
            RefreshToken refreshToken = AuthenticationFixture.aRefreshToken();

            given(authenticationFactory.createRefreshToken(command)).willReturn(refreshToken);
            given(
                            tokenRefreshCoordinator.coordinate(
                                    command.tenantId(), command.userId(), refreshToken))
                    .willReturn(
                            Mono.error(
                                    new TokenRefreshFailedException(
                                            "Failed to acquire lock for token refresh")));

            // when
            Mono<RefreshAccessTokenResponse> result = refreshAccessTokenService.execute(command);

            // then
            StepVerifier.create(result).expectError(TokenRefreshFailedException.class).verify();
        }
    }
}
