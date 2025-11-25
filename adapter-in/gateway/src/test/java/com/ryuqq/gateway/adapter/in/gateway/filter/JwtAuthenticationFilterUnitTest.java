package com.ryuqq.gateway.adapter.in.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ryuqq.gateway.adapter.in.gateway.config.GatewayFilterOrder;
import com.ryuqq.gateway.application.authentication.dto.command.ValidateJwtCommand;
import com.ryuqq.gateway.application.authentication.dto.response.ValidateJwtResponse;
import com.ryuqq.gateway.application.authentication.port.in.command.ValidateJwtUseCase;
import com.ryuqq.gateway.domain.authentication.vo.JwtClaims;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * JwtAuthenticationFilter Unit 테스트
 *
 * <p>Filter 로직의 상세 검증을 위한 단위 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterUnitTest {

    @Mock private ValidateJwtUseCase validateJwtUseCase;

    @Mock private GatewayFilterChain filterChain;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        jwtAuthenticationFilter = new JwtAuthenticationFilter(validateJwtUseCase, objectMapper);
    }

    @Nested
    @DisplayName("Filter Order 검증")
    class FilterOrderTest {

        @Test
        @DisplayName("Filter Order는 GatewayFilterOrder.JWT_AUTH_FILTER와 동일해야 한다")
        void shouldReturnCorrectFilterOrder() {
            assertThat(jwtAuthenticationFilter.getOrder())
                    .isEqualTo(GatewayFilterOrder.JWT_AUTH_FILTER);
        }
    }

    @Nested
    @DisplayName("Bearer Token 추출")
    class BearerTokenExtractionTest {

        @Test
        @DisplayName("유효한 Bearer Token을 정확하게 추출해야 한다")
        void shouldExtractValidBearerToken() {
            // given
            String expectedToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.payload.signature";
            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/test")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + expectedToken)
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            JwtClaims claims =
                    JwtClaims.of(
                            "user-123",
                            "auth-hub",
                            Instant.now().plusSeconds(3600),
                            Instant.now(),
                            List.of("ROLE_USER"));
            when(validateJwtUseCase.execute(any(ValidateJwtCommand.class)))
                    .thenReturn(Mono.just(new ValidateJwtResponse(claims, true)));
            when(filterChain.filter(exchange)).thenReturn(Mono.empty());

            // when
            StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                    .verifyComplete();

            // then
            ArgumentCaptor<ValidateJwtCommand> captor =
                    ArgumentCaptor.forClass(ValidateJwtCommand.class);
            verify(validateJwtUseCase).execute(captor.capture());
            assertThat(captor.getValue().accessToken()).isEqualTo(expectedToken);
        }

        @Test
        @DisplayName("Bearer prefix가 없으면 401을 반환해야 한다")
        void shouldReturn401WhenBearerPrefixMissing() {
            // given
            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/test")
                            .header(HttpHeaders.AUTHORIZATION, "Basic abc123")
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            // when
            StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                    .verifyComplete();

            // then
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            verify(validateJwtUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("Exchange Attributes 설정")
    class ExchangeAttributesTest {

        @Test
        @DisplayName("검증 성공 시 userId를 Exchange Attribute에 설정해야 한다")
        void shouldSetUserIdAttribute() {
            // given
            String token = "valid-token";
            String expectedUserId = "user-456";
            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/test")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            JwtClaims claims =
                    JwtClaims.of(
                            expectedUserId,
                            "auth-hub",
                            Instant.now().plusSeconds(3600),
                            Instant.now(),
                            List.of());
            when(validateJwtUseCase.execute(any(ValidateJwtCommand.class)))
                    .thenReturn(Mono.just(new ValidateJwtResponse(claims, true)));
            when(filterChain.filter(exchange)).thenReturn(Mono.empty());

            // when
            StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                    .verifyComplete();

            // then
            assertThat((String) exchange.getAttribute("userId")).isEqualTo(expectedUserId);
        }

        @Test
        @DisplayName("검증 성공 시 roles를 Exchange Attribute에 설정해야 한다")
        void shouldSetRolesAttribute() {
            // given
            String token = "valid-token";
            List<String> expectedRoles = List.of("ROLE_ADMIN", "ROLE_USER");
            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/test")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            JwtClaims claims =
                    JwtClaims.of(
                            "user-123",
                            "auth-hub",
                            Instant.now().plusSeconds(3600),
                            Instant.now(),
                            expectedRoles);
            when(validateJwtUseCase.execute(any(ValidateJwtCommand.class)))
                    .thenReturn(Mono.just(new ValidateJwtResponse(claims, true)));
            when(filterChain.filter(exchange)).thenReturn(Mono.empty());

            // when
            StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                    .verifyComplete();

            // then
            @SuppressWarnings("unchecked")
            List<String> actualRoles = (List<String>) exchange.getAttribute("roles");
            assertThat(actualRoles).isEqualTo(expectedRoles);
        }
    }

    @Nested
    @DisplayName("Filter Chain 전달")
    class FilterChainTest {

        @Test
        @DisplayName("검증 성공 시 다음 Filter로 전달해야 한다")
        void shouldPassToNextFilterOnSuccess() {
            // given
            String token = "valid-token";
            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/test")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            JwtClaims claims =
                    JwtClaims.of(
                            "user-123",
                            "auth-hub",
                            Instant.now().plusSeconds(3600),
                            Instant.now(),
                            List.of());
            when(validateJwtUseCase.execute(any(ValidateJwtCommand.class)))
                    .thenReturn(Mono.just(new ValidateJwtResponse(claims, true)));
            when(filterChain.filter(exchange)).thenReturn(Mono.empty());

            // when
            StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                    .verifyComplete();

            // then
            verify(filterChain).filter(exchange);
        }

        @Test
        @DisplayName("검증 실패 시 다음 Filter로 전달하지 않아야 한다")
        void shouldNotPassToNextFilterOnFailure() {
            // given
            String token = "invalid-token";
            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/test")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            when(validateJwtUseCase.execute(any(ValidateJwtCommand.class)))
                    .thenReturn(Mono.just(new ValidateJwtResponse(null, false)));

            // when
            StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                    .verifyComplete();

            // then
            verify(filterChain, never()).filter(any());
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("에러 처리")
    class ErrorHandlingTest {

        @Test
        @DisplayName("UseCase 에러 발생 시 401을 반환해야 한다")
        void shouldReturn401OnUseCaseError() {
            // given
            String token = "error-token";
            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/test")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            when(validateJwtUseCase.execute(any(ValidateJwtCommand.class)))
                    .thenReturn(Mono.error(new RuntimeException("Validation error")));

            // when
            StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                    .verifyComplete();

            // then
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            verify(filterChain, never()).filter(any());
        }
    }
}
