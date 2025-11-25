package com.ryuqq.gateway.adapter.in.gateway.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ryuqq.gateway.domain.authentication.exception.JwtExpiredException;
import com.ryuqq.gateway.domain.authentication.exception.JwtInvalidException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

/**
 * JwtErrorHandler 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
class JwtErrorHandlerTest {

    private JwtErrorHandler jwtErrorHandler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        jwtErrorHandler = new JwtErrorHandler(objectMapper);
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

    @Test
    @DisplayName("에러 응답에 traceId가 포함되어야 한다")
    void shouldIncludeTraceIdInErrorResponse() {
        // given
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        String expectedTraceId = "trace-789";
        exchange.getAttributes().put("traceId", expectedTraceId);

        JwtInvalidException exception = new JwtInvalidException("Invalid JWT");

        // when
        StepVerifier.create(jwtErrorHandler.handle(exchange, exception)).verifyComplete();

        // then
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
