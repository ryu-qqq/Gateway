package com.ryuqq.gateway.adapter.in.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
 * JwtAuthenticationFilter 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private ValidateJwtUseCase validateJwtUseCase;

    @Mock private GatewayFilterChain filterChain;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        jwtAuthenticationFilter = new JwtAuthenticationFilter(validateJwtUseCase, objectMapper);
    }

    @Test
    @DisplayName("Filter Order는 JWT_AUTH_FILTER(2)여야 한다")
    void shouldHaveCorrectFilterOrder() {
        // when
        int order = jwtAuthenticationFilter.getOrder();

        // then
        assertThat(order).isEqualTo(GatewayFilterOrder.JWT_AUTH_FILTER);
    }

    @Test
    @DisplayName("Authorization 헤더에서 Bearer Token을 추출해야 한다")
    void shouldExtractBearerTokenFromAuthorizationHeader() {
        // given
        String token = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.test";
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
                        List.of("ROLE_USER"));
        ValidateJwtResponse response = new ValidateJwtResponse(claims, true);
        when(validateJwtUseCase.execute(any(ValidateJwtCommand.class)))
                .thenReturn(Mono.just(response));
        when(filterChain.filter(exchange)).thenReturn(Mono.empty());

        // when & then
        StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain)).verifyComplete();
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 401을 반환해야 한다")
    void shouldReturn401WhenAuthorizationHeaderMissing() {
        // given
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // when
        Mono<Void> result = jwtAuthenticationFilter.filter(exchange, filterChain);

        // then
        StepVerifier.create(result).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("JWT 검증 후 Exchange Attributes를 설정해야 한다")
    void shouldValidateJwtAndSetAttributes() {
        // given
        String token = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.test";
        MockServerHttpRequest request =
                MockServerHttpRequest.get("/api/test")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        String subject = "user-123";
        List<String> roles = List.of("ROLE_USER", "ROLE_ADMIN");

        JwtClaims claims =
                JwtClaims.of(
                        subject, "auth-hub", Instant.now().plusSeconds(3600), Instant.now(), roles);
        ValidateJwtResponse response = new ValidateJwtResponse(claims, true);
        when(validateJwtUseCase.execute(any(ValidateJwtCommand.class)))
                .thenReturn(Mono.just(response));
        when(filterChain.filter(exchange)).thenReturn(Mono.empty());

        // when
        StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain)).verifyComplete();

        // then
        assertThat((String) exchange.getAttribute("userId")).isEqualTo(subject);
        @SuppressWarnings("unchecked")
        List<String> actualRoles = (List<String>) exchange.getAttribute("roles");
        assertThat(actualRoles).isEqualTo(roles);
    }

    @Test
    @DisplayName("JWT 검증 실패 시 401을 반환해야 한다")
    void shouldReturn401WhenJwtValidationFails() {
        // given
        String token = "invalid-token";
        MockServerHttpRequest request =
                MockServerHttpRequest.get("/api/test")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        ValidateJwtResponse failedResponse = new ValidateJwtResponse(null, false);
        when(validateJwtUseCase.execute(any(ValidateJwtCommand.class)))
                .thenReturn(Mono.just(failedResponse));

        // when
        Mono<Void> result = jwtAuthenticationFilter.filter(exchange, filterChain);

        // then
        StepVerifier.create(result).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
