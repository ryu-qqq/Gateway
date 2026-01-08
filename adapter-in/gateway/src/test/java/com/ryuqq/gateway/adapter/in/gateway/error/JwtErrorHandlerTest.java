package com.ryuqq.gateway.adapter.in.gateway.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import com.ryuqq.gateway.adapter.in.gateway.common.util.GatewayErrorResponder;
import com.ryuqq.gateway.domain.authentication.exception.JwtExpiredException;
import com.ryuqq.gateway.domain.authentication.exception.JwtInvalidException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * JwtErrorHandler 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class JwtErrorHandlerTest {

    @Mock private GatewayErrorResponder errorResponder;

    private JwtErrorHandler jwtErrorHandler;

    @BeforeEach
    void setUp() {
        lenient()
                .when(errorResponder.respond(any(), any(HttpStatus.class), any(), any()))
                .thenAnswer(
                        invocation -> {
                            MockServerWebExchange exchange = invocation.getArgument(0);
                            HttpStatusCode status = invocation.getArgument(1);
                            exchange.getResponse().setStatusCode(status);
                            return Mono.empty();
                        });
        jwtErrorHandler = new JwtErrorHandler(errorResponder);
    }

    @Test
    @DisplayName("JwtExpiredException 발생 시 401을 반환해야 한다")
    void shouldHandle401ForJwtExpiredException() {
        // given
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getAttributes().put("traceId", "trace-123");

        JwtExpiredException exception = new JwtExpiredException("JWT has expired");

        // when
        StepVerifier.create(jwtErrorHandler.handle(exchange, exception)).verifyComplete();

        // then
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("JwtInvalidException 발생 시 401을 반환해야 한다")
    void shouldHandle401ForJwtInvalidException() {
        // given
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getAttributes().put("traceId", "trace-456");

        JwtInvalidException exception = new JwtInvalidException("Invalid JWT signature");

        // when
        StepVerifier.create(jwtErrorHandler.handle(exchange, exception)).verifyComplete();

        // then
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("알 수 없는 예외 발생 시 500을 반환해야 한다")
    void shouldHandle500ForUnknownException() {
        // given
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        RuntimeException exception = new RuntimeException("Unknown error");

        // when
        StepVerifier.create(jwtErrorHandler.handle(exchange, exception)).verifyComplete();

        // then
        assertThat(exchange.getResponse().getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
