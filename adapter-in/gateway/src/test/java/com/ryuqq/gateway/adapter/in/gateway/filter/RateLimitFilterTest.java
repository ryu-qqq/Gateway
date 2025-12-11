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
import com.ryuqq.gateway.domain.ratelimit.exception.IpBlockedException;
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
 * RateLimitFilter 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitFilter 테스트")
class RateLimitFilterTest {

    @Mock
    private CheckRateLimitUseCase checkRateLimitUseCase;

    @Mock
    private GatewayFilterChain filterChain;

    private RateLimitFilter rateLimitFilter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        rateLimitFilter = new RateLimitFilter(checkRateLimitUseCase, objectMapper);
    }

    @Nested
    @DisplayName("Filter Order 테스트")
    class FilterOrderTest {

        @Test
        @DisplayName("Filter Order는 RATE_LIMIT_FILTER(1)여야 한다")
        void shouldHaveCorrectFilterOrder() {
            // when
            int order = rateLimitFilter.getOrder();

            // then
            assertThat(order).isEqualTo(GatewayFilterOrder.RATE_LIMIT_FILTER);
        }
    }

    @Nested
    @DisplayName("IP Rate Limit 테스트")
    class IpRateLimitTest {

        @Test
        @DisplayName("IP Rate Limit 허용 시 다음 필터로 진행")
        void shouldProceedWhenIpRateLimitAllowed() {
            // given
            MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
                    .header("X-Forwarded-For", "192.168.1.1")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            CheckRateLimitResponse ipResponse = CheckRateLimitResponse.allowed(5, 100);
            CheckRateLimitResponse endpointResponse = CheckRateLimitResponse.allowed(10, 1000);

            when(checkRateLimitUseCase.execute(argThat(cmd ->
                    cmd != null && cmd.limitType() == LimitType.IP)))
                    .thenReturn(Mono.just(ipResponse));
            when(checkRateLimitUseCase.execute(argThat(cmd ->
                    cmd != null && cmd.limitType() == LimitType.ENDPOINT)))
                    .thenReturn(Mono.just(endpointResponse));
            when(filterChain.filter(exchange)).thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(rateLimitFilter.filter(exchange, filterChain))
                    .verifyComplete();

            verify(filterChain).filter(exchange);
        }

        @Test
        @DisplayName("IP Rate Limit 초과 시 429 반환")
        void shouldReturn429WhenIpRateLimitExceeded() {
            // given
            MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
                    .header("X-Forwarded-For", "192.168.1.1")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            CheckRateLimitResponse deniedResponse = CheckRateLimitResponse.denied(
                    100, 100, 60, RateLimitAction.REJECT);

            when(checkRateLimitUseCase.execute(any(CheckRateLimitCommand.class)))
                    .thenReturn(Mono.just(deniedResponse));

            // when
            Mono<Void> result = rateLimitFilter.filter(exchange, filterChain);

            // then
            StepVerifier.create(result).verifyComplete();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
            assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Limit")).isEqualTo("100");
            assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Remaining")).isEqualTo("0");
            assertThat(exchange.getResponse().getHeaders().getFirst("Retry-After")).isEqualTo("60");
            verify(filterChain, never()).filter(any());
        }
    }

    @Nested
    @DisplayName("Endpoint Rate Limit 테스트")
    class EndpointRateLimitTest {

        @Test
        @DisplayName("Endpoint Rate Limit 초과 시 429 반환")
        void shouldReturn429WhenEndpointRateLimitExceeded() {
            // given
            MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
                    .header("X-Forwarded-For", "192.168.1.1")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            CheckRateLimitResponse ipResponse = CheckRateLimitResponse.allowed(5, 100);
            CheckRateLimitResponse endpointDenied = CheckRateLimitResponse.denied(
                    1000, 1000, 30, RateLimitAction.REJECT);

            when(checkRateLimitUseCase.execute(argThat(cmd ->
                    cmd != null && cmd.limitType() == LimitType.IP)))
                    .thenReturn(Mono.just(ipResponse));
            when(checkRateLimitUseCase.execute(argThat(cmd ->
                    cmd != null && cmd.limitType() == LimitType.ENDPOINT)))
                    .thenReturn(Mono.just(endpointDenied));

            // when
            Mono<Void> result = rateLimitFilter.filter(exchange, filterChain);

            // then
            StepVerifier.create(result).verifyComplete();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
            assertThat(exchange.getResponse().getHeaders().getFirst("Retry-After")).isEqualTo("30");
            verify(filterChain, never()).filter(any());
        }
    }

    @Nested
    @DisplayName("Rate Limit 헤더 테스트")
    class RateLimitHeaderTest {

        @Test
        @DisplayName("Rate Limit 허용 시 헤더 추가")
        void shouldAddRateLimitHeadersWhenAllowed() {
            // given
            MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
                    .header("X-Forwarded-For", "192.168.1.1")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            CheckRateLimitResponse ipResponse = CheckRateLimitResponse.allowed(5, 100);
            CheckRateLimitResponse endpointResponse = CheckRateLimitResponse.allowed(10, 1000);

            when(checkRateLimitUseCase.execute(argThat(cmd ->
                    cmd != null && cmd.limitType() == LimitType.IP)))
                    .thenReturn(Mono.just(ipResponse));
            when(checkRateLimitUseCase.execute(argThat(cmd ->
                    cmd != null && cmd.limitType() == LimitType.ENDPOINT)))
                    .thenReturn(Mono.just(endpointResponse));
            when(filterChain.filter(exchange)).thenReturn(Mono.empty());

            // when
            StepVerifier.create(rateLimitFilter.filter(exchange, filterChain))
                    .verifyComplete();

            // then
            assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Limit")).isEqualTo("1000");
            assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Remaining")).isEqualTo("990");
        }
    }

    @Nested
    @DisplayName("예외 처리 테스트")
    class ExceptionHandlingTest {

        @Test
        @DisplayName("IpBlockedException 발생 시 403 반환")
        void shouldReturn403WhenIpBlocked() {
            // given
            MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
                    .header("X-Forwarded-For", "192.168.1.1")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            when(checkRateLimitUseCase.execute(any(CheckRateLimitCommand.class)))
                    .thenReturn(Mono.error(new IpBlockedException("192.168.1.1", 3600)));

            // when
            Mono<Void> result = rateLimitFilter.filter(exchange, filterChain);

            // then
            StepVerifier.create(result).verifyComplete();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(exchange.getResponse().getHeaders().getFirst("Retry-After")).isEqualTo("3600");
            verify(filterChain, never()).filter(any());
        }

        @Test
        @DisplayName("RateLimitExceededException 발생 시 429 반환")
        void shouldReturn429WhenRateLimitExceeded() {
            // given
            MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
                    .header("X-Forwarded-For", "192.168.1.1")
                    .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            when(checkRateLimitUseCase.execute(any(CheckRateLimitCommand.class)))
                    .thenReturn(Mono.error(new RateLimitExceededException(100, 0, 120)));

            // when
            Mono<Void> result = rateLimitFilter.filter(exchange, filterChain);

            // then
            StepVerifier.create(result).verifyComplete();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
            assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Limit")).isEqualTo("100");
            assertThat(exchange.getResponse().getHeaders().getFirst("Retry-After")).isEqualTo("120");
            verify(filterChain, never()).filter(any());
        }
    }
}
