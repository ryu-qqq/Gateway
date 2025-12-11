package com.ryuqq.gateway.adapter.in.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ryuqq.gateway.adapter.in.gateway.config.GatewayFilterOrder;
import com.ryuqq.gateway.application.authentication.dto.command.RefreshAccessTokenCommand;
import com.ryuqq.gateway.application.authentication.dto.response.RefreshAccessTokenResponse;
import com.ryuqq.gateway.application.authentication.port.in.command.RefreshAccessTokenUseCase;
import com.ryuqq.gateway.application.authentication.port.out.client.AuthHubClient;
import com.ryuqq.gateway.domain.authentication.exception.RefreshTokenExpiredException;
import com.ryuqq.gateway.domain.authentication.exception.RefreshTokenInvalidException;
import com.ryuqq.gateway.domain.authentication.exception.RefreshTokenMissingException;
import com.ryuqq.gateway.domain.authentication.exception.RefreshTokenReusedException;
import com.ryuqq.gateway.domain.authentication.exception.TokenRefreshFailedException;
import com.ryuqq.gateway.domain.authentication.vo.ExpiredTokenInfo;
import com.ryuqq.gateway.domain.authentication.vo.TokenPair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenRefreshFilter 테스트")
class TokenRefreshFilterTest {

    @Mock private RefreshAccessTokenUseCase refreshAccessTokenUseCase;
    @Mock private AuthHubClient authHubClient;
    @Mock private GatewayFilterChain chain;

    private ObjectMapper objectMapper;
    private TokenRefreshFilter tokenRefreshFilter;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        tokenRefreshFilter =
                new TokenRefreshFilter(refreshAccessTokenUseCase, authHubClient, objectMapper);
    }

    @Nested
    @DisplayName("getOrder() 테스트")
    class GetOrderTest {

        @Test
        @DisplayName("올바른 필터 순서 반환")
        void shouldReturnCorrectFilterOrder() {
            // when
            int order = tokenRefreshFilter.getOrder();

            // then
            assertThat(order).isEqualTo(GatewayFilterOrder.TOKEN_REFRESH_FILTER);
        }
    }

    @Nested
    @DisplayName("filter() 테스트 - 스킵 조건")
    class FilterSkipTest {

        @Test
        @DisplayName("Authorization 헤더가 없으면 다음 필터로 진행")
        void shouldProceedWhenNoAuthorizationHeader() {
            // given
            ServerWebExchange exchange = createExchangeWithoutAuth("/api/v1/users");
            when(chain.filter(exchange)).thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(tokenRefreshFilter.filter(exchange, chain)).verifyComplete();

            verify(chain).filter(exchange);
            verify(authHubClient, never()).extractExpiredTokenInfo(anyString());
        }

        @Test
        @DisplayName("Bearer 토큰이 아니면 다음 필터로 진행")
        void shouldProceedWhenNotBearerToken() {
            // given
            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/v1/users")
                            .header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz")
                            .build();
            ServerWebExchange exchange = MockServerWebExchange.from(request);
            when(chain.filter(exchange)).thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(tokenRefreshFilter.filter(exchange, chain)).verifyComplete();

            verify(chain).filter(exchange);
            verify(authHubClient, never()).extractExpiredTokenInfo(anyString());
        }

        @Test
        @DisplayName("refresh_token 쿠키가 없으면 다음 필터로 진행")
        void shouldProceedWhenNoRefreshTokenCookie() {
            // given
            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/v1/users")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer access-token-123")
                            .build();
            ServerWebExchange exchange = MockServerWebExchange.from(request);
            when(chain.filter(exchange)).thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(tokenRefreshFilter.filter(exchange, chain)).verifyComplete();

            verify(chain).filter(exchange);
            verify(authHubClient, never()).extractExpiredTokenInfo(anyString());
        }

        @Test
        @DisplayName("Access Token이 만료되지 않았으면 다음 필터로 진행")
        void shouldProceedWhenAccessTokenNotExpired() {
            // given
            ServerWebExchange exchange = createExchangeWithAuthAndCookie("/api/v1/users");
            ExpiredTokenInfo notExpiredToken = new ExpiredTokenInfo(false, 1L, "tenant-1");
            when(authHubClient.extractExpiredTokenInfo("access-token-123"))
                    .thenReturn(Mono.just(notExpiredToken));
            when(chain.filter(exchange)).thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(tokenRefreshFilter.filter(exchange, chain)).verifyComplete();

            verify(chain).filter(exchange);
            verify(refreshAccessTokenUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("만료된 토큰에서 userId 또는 tenantId를 추출할 수 없으면 다음 필터로 진행")
        void shouldProceedWhenCannotExtractUserIdOrTenantId() {
            // given
            ServerWebExchange exchange = createExchangeWithAuthAndCookie("/api/v1/users");
            ExpiredTokenInfo expiredTokenWithoutIds = new ExpiredTokenInfo(true, null, null);
            when(authHubClient.extractExpiredTokenInfo("access-token-123"))
                    .thenReturn(Mono.just(expiredTokenWithoutIds));
            when(chain.filter(exchange)).thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(tokenRefreshFilter.filter(exchange, chain)).verifyComplete();

            verify(chain).filter(exchange);
            verify(refreshAccessTokenUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("AuthHub 호출 실패 시 다음 필터로 진행")
        void shouldProceedWhenAuthHubCallFails() {
            // given
            ServerWebExchange exchange = createExchangeWithAuthAndCookie("/api/v1/users");
            when(authHubClient.extractExpiredTokenInfo("access-token-123"))
                    .thenReturn(Mono.error(new RuntimeException("AuthHub unavailable")));
            when(chain.filter(exchange)).thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(tokenRefreshFilter.filter(exchange, chain)).verifyComplete();

            verify(chain).filter(exchange);
        }
    }

    @Nested
    @DisplayName("filter() 테스트 - 토큰 갱신 성공")
    class TokenRefreshSuccessTest {

        @Test
        @DisplayName("토큰 갱신 성공 시 새 토큰으로 요청 변환")
        void shouldMutateRequestWithNewTokensOnSuccess() {
            // given
            ServerWebExchange exchange = createExchangeWithAuthAndCookie("/api/v1/users");
            ExpiredTokenInfo expiredToken = new ExpiredTokenInfo(true, 123L, "tenant-1");

            // mock TokenPair 생성
            TokenPair mockTokenPair = org.mockito.Mockito.mock(TokenPair.class);
            when(mockTokenPair.accessTokenValue()).thenReturn("new-access-token");
            when(mockTokenPair.refreshTokenValue()).thenReturn("new-refresh-token");

            RefreshAccessTokenResponse refreshResponse =
                    new RefreshAccessTokenResponse(mockTokenPair);

            when(authHubClient.extractExpiredTokenInfo("access-token-123"))
                    .thenReturn(Mono.just(expiredToken));
            when(refreshAccessTokenUseCase.execute(any(RefreshAccessTokenCommand.class)))
                    .thenReturn(Mono.just(refreshResponse));
            when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(tokenRefreshFilter.filter(exchange, chain)).verifyComplete();

            ArgumentCaptor<RefreshAccessTokenCommand> commandCaptor =
                    ArgumentCaptor.forClass(RefreshAccessTokenCommand.class);
            verify(refreshAccessTokenUseCase).execute(commandCaptor.capture());

            RefreshAccessTokenCommand capturedCommand = commandCaptor.getValue();
            assertThat(capturedCommand.tenantId()).isEqualTo("tenant-1");
            assertThat(capturedCommand.userId()).isEqualTo(123L);
            assertThat(capturedCommand.refreshToken()).isEqualTo("refresh-token-456");

            verify(chain).filter(any(ServerWebExchange.class));
        }
    }

    @Nested
    @DisplayName("filter() 테스트 - 토큰 갱신 실패")
    class TokenRefreshFailureTest {

        @Test
        @DisplayName("RefreshTokenReusedException 발생 시 401 응답 - 토큰 재사용 감지")
        void shouldReturnUnauthorizedOnTokenReuseDetected() {
            // given
            ServerWebExchange exchange = createExchangeWithAuthAndCookie("/api/v1/users");
            ExpiredTokenInfo expiredToken = new ExpiredTokenInfo(true, 123L, "tenant-1");

            when(authHubClient.extractExpiredTokenInfo("access-token-123"))
                    .thenReturn(Mono.just(expiredToken));
            when(refreshAccessTokenUseCase.execute(any(RefreshAccessTokenCommand.class)))
                    .thenReturn(Mono.error(new RefreshTokenReusedException("token-id-123")));

            // when & then
            StepVerifier.create(tokenRefreshFilter.filter(exchange, chain)).verifyComplete();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("RefreshTokenExpiredException 발생 시 401 응답")
        void shouldReturnUnauthorizedOnExpiredRefreshToken() {
            // given
            ServerWebExchange exchange = createExchangeWithAuthAndCookie("/api/v1/users");
            ExpiredTokenInfo expiredToken = new ExpiredTokenInfo(true, 123L, "tenant-1");

            when(authHubClient.extractExpiredTokenInfo("access-token-123"))
                    .thenReturn(Mono.just(expiredToken));
            when(refreshAccessTokenUseCase.execute(any(RefreshAccessTokenCommand.class)))
                    .thenReturn(Mono.error(new RefreshTokenExpiredException()));

            // when & then
            StepVerifier.create(tokenRefreshFilter.filter(exchange, chain)).verifyComplete();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("RefreshTokenInvalidException 발생 시 401 응답")
        void shouldReturnUnauthorizedOnInvalidRefreshToken() {
            // given
            ServerWebExchange exchange = createExchangeWithAuthAndCookie("/api/v1/users");
            ExpiredTokenInfo expiredToken = new ExpiredTokenInfo(true, 123L, "tenant-1");

            when(authHubClient.extractExpiredTokenInfo("access-token-123"))
                    .thenReturn(Mono.just(expiredToken));
            when(refreshAccessTokenUseCase.execute(any(RefreshAccessTokenCommand.class)))
                    .thenReturn(Mono.error(new RefreshTokenInvalidException("Invalid signature")));

            // when & then
            StepVerifier.create(tokenRefreshFilter.filter(exchange, chain)).verifyComplete();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("RefreshTokenMissingException 발생 시 401 응답")
        void shouldReturnUnauthorizedOnMissingRefreshToken() {
            // given
            ServerWebExchange exchange = createExchangeWithAuthAndCookie("/api/v1/users");
            ExpiredTokenInfo expiredToken = new ExpiredTokenInfo(true, 123L, "tenant-1");

            when(authHubClient.extractExpiredTokenInfo("access-token-123"))
                    .thenReturn(Mono.just(expiredToken));
            when(refreshAccessTokenUseCase.execute(any(RefreshAccessTokenCommand.class)))
                    .thenReturn(Mono.error(new RefreshTokenMissingException()));

            // when & then
            StepVerifier.create(tokenRefreshFilter.filter(exchange, chain)).verifyComplete();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("TokenRefreshFailedException 발생 시 500 응답")
        void shouldReturnServerErrorOnTokenRefreshFailed() {
            // given
            ServerWebExchange exchange = createExchangeWithAuthAndCookie("/api/v1/users");
            ExpiredTokenInfo expiredToken = new ExpiredTokenInfo(true, 123L, "tenant-1");

            when(authHubClient.extractExpiredTokenInfo("access-token-123"))
                    .thenReturn(Mono.just(expiredToken));
            when(refreshAccessTokenUseCase.execute(any(RefreshAccessTokenCommand.class)))
                    .thenReturn(
                            Mono.error(
                                    new TokenRefreshFailedException(
                                            "tenant-1/user:123",
                                            new RuntimeException("DB error"))));

            // when & then
            StepVerifier.create(tokenRefreshFilter.filter(exchange, chain)).verifyComplete();

            assertThat(exchange.getResponse().getStatusCode())
                    .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Nested
    @DisplayName("에러 응답 테스트")
    class ErrorResponseTest {

        @Test
        @DisplayName("에러 응답에 JSON Content-Type 설정")
        void shouldSetJsonContentTypeOnErrorResponse() {
            // given
            ServerWebExchange exchange = createExchangeWithAuthAndCookie("/api/v1/users");
            ExpiredTokenInfo expiredToken = new ExpiredTokenInfo(true, 123L, "tenant-1");

            when(authHubClient.extractExpiredTokenInfo("access-token-123"))
                    .thenReturn(Mono.just(expiredToken));
            when(refreshAccessTokenUseCase.execute(any(RefreshAccessTokenCommand.class)))
                    .thenReturn(Mono.error(new RefreshTokenExpiredException()));

            // when
            StepVerifier.create(tokenRefreshFilter.filter(exchange, chain)).verifyComplete();

            // then
            assertThat(exchange.getResponse().getHeaders().getContentType())
                    .hasToString("application/json");
        }

        @Test
        @DisplayName("JSON 직렬화 실패 시 setComplete 호출")
        void shouldCallSetCompleteWhenJsonSerializationFails() throws Exception {
            // given
            ObjectMapper mockObjectMapper = org.mockito.Mockito.mock(ObjectMapper.class);
            when(mockObjectMapper.writeValueAsBytes(any()))
                    .thenThrow(
                            new com.fasterxml.jackson.core.JsonProcessingException(
                                    "Mocked error") {});
            TokenRefreshFilter filterWithMockedMapper =
                    new TokenRefreshFilter(
                            refreshAccessTokenUseCase, authHubClient, mockObjectMapper);

            ServerWebExchange exchange = createExchangeWithAuthAndCookie("/api/v1/users");
            ExpiredTokenInfo expiredToken = new ExpiredTokenInfo(true, 123L, "tenant-1");

            when(authHubClient.extractExpiredTokenInfo("access-token-123"))
                    .thenReturn(Mono.just(expiredToken));
            when(refreshAccessTokenUseCase.execute(any(RefreshAccessTokenCommand.class)))
                    .thenReturn(Mono.error(new RefreshTokenExpiredException()));

            // when & then
            StepVerifier.create(filterWithMockedMapper.filter(exchange, chain)).verifyComplete();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    // Helper methods
    private ServerWebExchange createExchangeWithoutAuth(String path) {
        MockServerHttpRequest request = MockServerHttpRequest.get(path).build();
        return MockServerWebExchange.from(request);
    }

    private ServerWebExchange createExchangeWithAuthAndCookie(String path) {
        MockServerHttpRequest request =
                MockServerHttpRequest.get(path)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token-123")
                        .cookie(new HttpCookie("refresh_token", "refresh-token-456"))
                        .build();
        return MockServerWebExchange.from(request);
    }
}
