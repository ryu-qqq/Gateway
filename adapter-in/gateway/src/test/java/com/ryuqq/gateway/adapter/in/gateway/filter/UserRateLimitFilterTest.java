package com.ryuqq.gateway.adapter.in.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ryuqq.gateway.adapter.in.gateway.config.GatewayFilterOrder;
import com.ryuqq.gateway.application.ratelimit.dto.command.CheckRateLimitCommand;
import com.ryuqq.gateway.application.ratelimit.dto.response.CheckRateLimitResponse;
import com.ryuqq.gateway.application.ratelimit.port.in.command.CheckRateLimitUseCase;
import com.ryuqq.gateway.domain.ratelimit.exception.RateLimitExceededException;
import com.ryuqq.gateway.domain.ratelimit.vo.LimitType;
import com.ryuqq.gateway.domain.ratelimit.vo.RateLimitAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * UserRateLimitFilter 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserRateLimitFilter 테스트")
class UserRateLimitFilterTest {

    @Mock private CheckRateLimitUseCase checkRateLimitUseCase;

    @Mock private GatewayFilterChain filterChain;

    private UserRateLimitFilter userRateLimitFilter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        userRateLimitFilter = new UserRateLimitFilter(checkRateLimitUseCase, objectMapper);
    }

    @Nested
    @DisplayName("Filter Order 테스트")
    class FilterOrderTest {

        @Test
        @DisplayName("Filter Order는 USER_RATE_LIMIT_FILTER(4)여야 한다")
        void shouldHaveCorrectFilterOrder() {
            // when
            int order = userRateLimitFilter.getOrder();

            // then
            assertThat(order).isEqualTo(GatewayFilterOrder.USER_RATE_LIMIT_FILTER);
        }
    }

    @Nested
    @DisplayName("userId 없을 때 테스트")
    class NoUserIdTest {

        @Test
        @DisplayName("userId가 null이면 다음 필터로 통과")
        void shouldPassWhenUserIdIsNull() {
            // given
            MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            // userId attribute 설정하지 않음

            when(filterChain.filter(exchange)).thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(userRateLimitFilter.filter(exchange, filterChain)).verifyComplete();

            verify(filterChain).filter(exchange);
            verify(checkRateLimitUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("userId가 빈 문자열이면 다음 필터로 통과")
        void shouldPassWhenUserIdIsBlank() {
            // given
            MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            exchange.getAttributes().put("userId", "");

            when(filterChain.filter(exchange)).thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(userRateLimitFilter.filter(exchange, filterChain)).verifyComplete();

            verify(filterChain).filter(exchange);
            verify(checkRateLimitUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("userId가 공백만 있으면 다음 필터로 통과")
        void shouldPassWhenUserIdIsWhitespace() {
            // given
            MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            exchange.getAttributes().put("userId", "   ");

            when(filterChain.filter(exchange)).thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(userRateLimitFilter.filter(exchange, filterChain)).verifyComplete();

            verify(filterChain).filter(exchange);
            verify(checkRateLimitUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("User Rate Limit 체크 테스트")
    class UserRateLimitCheckTest {

        @Test
        @DisplayName("User Rate Limit 허용 시 다음 필터로 진행")
        void shouldProceedWhenUserRateLimitAllowed() {
            // given
            MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            exchange.getAttributes().put("userId", "user-123");

            CheckRateLimitResponse allowedResponse = CheckRateLimitResponse.allowed(50, 100);

            when(checkRateLimitUseCase.execute(
                            argThat(
                                    cmd ->
                                            cmd != null
                                                    && cmd.limitType() == LimitType.USER
                                                    && "user-123".equals(cmd.identifier()))))
                    .thenReturn(Mono.just(allowedResponse));
            when(filterChain.filter(exchange)).thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(userRateLimitFilter.filter(exchange, filterChain)).verifyComplete();

            verify(filterChain).filter(exchange);
        }

        @Test
        @DisplayName("User Rate Limit 초과 시 429 반환")
        void shouldReturn429WhenUserRateLimitExceeded() {
            // given
            MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            exchange.getAttributes().put("userId", "user-123");

            CheckRateLimitResponse deniedResponse =
                    CheckRateLimitResponse.denied(100, 100, 60, RateLimitAction.REJECT);

            when(checkRateLimitUseCase.execute(any(CheckRateLimitCommand.class)))
                    .thenReturn(Mono.just(deniedResponse));

            // when
            Mono<Void> result = userRateLimitFilter.filter(exchange, filterChain);

            // then
            StepVerifier.create(result).verifyComplete();

            assertThat(exchange.getResponse().getStatusCode())
                    .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
            assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Limit"))
                    .isEqualTo("100");
            assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Remaining"))
                    .isEqualTo("0");
            assertThat(exchange.getResponse().getHeaders().getFirst("Retry-After")).isEqualTo("60");
            verify(filterChain, never()).filter(any());
        }
    }

    @Nested
    @DisplayName("Rate Limit 헤더 테스트")
    class RateLimitHeaderTest {

        @Test
        @DisplayName("Rate Limit 허용 시 헤더 추가 (기존 헤더 덮어쓰기)")
        void shouldSetRateLimitHeadersWhenAllowed() {
            // given
            MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            exchange.getAttributes().put("userId", "user-123");

            // 기존 헤더가 있다고 가정
            exchange.getResponse().getHeaders().add("X-RateLimit-Limit", "1000");
            exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", "990");

            CheckRateLimitResponse allowedResponse = CheckRateLimitResponse.allowed(50, 100);

            when(checkRateLimitUseCase.execute(any(CheckRateLimitCommand.class)))
                    .thenReturn(Mono.just(allowedResponse));
            when(filterChain.filter(exchange)).thenReturn(Mono.empty());

            // when
            StepVerifier.create(userRateLimitFilter.filter(exchange, filterChain)).verifyComplete();

            // then - User Rate Limit 값으로 덮어쓰기됨
            assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Limit"))
                    .isEqualTo("100");
            assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Remaining"))
                    .isEqualTo("50");
        }
    }

    @Nested
    @DisplayName("예외 처리 테스트")
    class ExceptionHandlingTest {

        @Test
        @DisplayName("RateLimitExceededException 발생 시 429 반환")
        void shouldReturn429WhenRateLimitExceeded() {
            // given
            MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            exchange.getAttributes().put("userId", "user-123");

            when(checkRateLimitUseCase.execute(any(CheckRateLimitCommand.class)))
                    .thenReturn(Mono.error(new RateLimitExceededException(100, 0, 120)));

            // when
            Mono<Void> result = userRateLimitFilter.filter(exchange, filterChain);

            // then
            StepVerifier.create(result).verifyComplete();

            assertThat(exchange.getResponse().getStatusCode())
                    .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
            assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Limit"))
                    .isEqualTo("100");
            assertThat(exchange.getResponse().getHeaders().getFirst("Retry-After"))
                    .isEqualTo("120");
            verify(filterChain, never()).filter(any());
        }

        @Test
        @DisplayName("일반 예외 발생 시 fail-open (요청 통과)")
        void shouldPassWhenGenericExceptionOccurs() {
            // given
            MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            exchange.getAttributes().put("userId", "user-123");

            when(checkRateLimitUseCase.execute(any(CheckRateLimitCommand.class)))
                    .thenReturn(Mono.error(new RuntimeException("Redis connection failed")));
            when(filterChain.filter(exchange)).thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(userRateLimitFilter.filter(exchange, filterChain)).verifyComplete();

            // fail-open: 요청 통과
            verify(filterChain).filter(exchange);
        }
    }
}
