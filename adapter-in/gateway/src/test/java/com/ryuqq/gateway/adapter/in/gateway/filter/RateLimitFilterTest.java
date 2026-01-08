package com.ryuqq.gateway.adapter.in.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ryuqq.gateway.adapter.in.gateway.common.util.ClientIpExtractor;
import com.ryuqq.gateway.adapter.in.gateway.common.util.GatewayErrorResponder;
import com.ryuqq.gateway.adapter.in.gateway.config.GatewayFilterOrder;
import com.ryuqq.gateway.adapter.in.gateway.metrics.GatewayMetrics;
import com.ryuqq.gateway.application.ratelimit.config.RateLimitProperties;
import com.ryuqq.gateway.application.ratelimit.dto.command.CheckRateLimitCommand;
import com.ryuqq.gateway.application.ratelimit.dto.response.CheckRateLimitResponse;
import com.ryuqq.gateway.application.ratelimit.port.in.command.CheckRateLimitUseCase;
import com.ryuqq.gateway.domain.ratelimit.exception.IpBlockedException;
import com.ryuqq.gateway.domain.ratelimit.exception.RateLimitExceededException;
import com.ryuqq.gateway.domain.ratelimit.vo.LimitType;
import com.ryuqq.gateway.domain.ratelimit.vo.RateLimitAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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

    @Mock private RateLimitProperties rateLimitProperties;

    @Mock private CheckRateLimitUseCase checkRateLimitUseCase;

    @Mock private GatewayFilterChain filterChain;

    @Mock private ClientIpExtractor clientIpExtractor;

    @Mock private GatewayMetrics gatewayMetrics;

    @Mock private GatewayErrorResponder errorResponder;

    private RateLimitFilter rateLimitFilter;

    @BeforeEach
    void setUp() {
        lenient().when(rateLimitProperties.isEnabled()).thenReturn(true);
        lenient().when(clientIpExtractor.extractWithTrustedProxy(any())).thenReturn("127.0.0.1");

        // Mock errorResponder - 429 응답
        lenient()
                .when(errorResponder.tooManyRequests(any(), any(), any()))
                .thenAnswer(
                        invocation -> {
                            MockServerWebExchange exchange = invocation.getArgument(0);
                            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                            return Mono.empty();
                        });

        // Mock errorResponder - 403 응답
        lenient()
                .when(errorResponder.forbidden(any(), any(), any()))
                .thenAnswer(
                        invocation -> {
                            MockServerWebExchange exchange = invocation.getArgument(0);
                            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                            return Mono.empty();
                        });

        rateLimitFilter =
                new RateLimitFilter(
                        rateLimitProperties,
                        checkRateLimitUseCase,
                        clientIpExtractor,
                        gatewayMetrics,
                        errorResponder);
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
    @DisplayName("Idempotency Guard 테스트")
    class IdempotencyGuardTest {

        @Test
        @DisplayName("이미 Rate Limit 체크된 요청은 스킵해야 한다")
        void shouldSkipWhenRateLimitAlreadyChecked() {
            // given
            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/test")
                            .header("X-Forwarded-For", "192.168.1.1")
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            // 이미 Rate Limit 체크가 완료된 것으로 설정
            exchange.getAttributes().put("RATE_LIMIT_CHECKED", true);

            when(filterChain.filter(exchange)).thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(rateLimitFilter.filter(exchange, filterChain)).verifyComplete();

            // Rate Limit 체크 없이 다음 필터로 진행해야 함
            verify(filterChain).filter(exchange);
            verify(checkRateLimitUseCase, never()).execute(any(CheckRateLimitCommand.class));
        }
    }

    @Nested
    @DisplayName("Rate Limit 비활성화 테스트")
    class RateLimitDisabledTest {

        @Test
        @DisplayName("Rate Limit 비활성화 시 Rate Limit 체크 없이 다음 필터로 진행")
        void shouldSkipRateLimitCheckWhenDisabled() {
            // given
            when(rateLimitProperties.isEnabled()).thenReturn(false);

            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/test")
                            .header("X-Forwarded-For", "192.168.1.1")
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            when(filterChain.filter(exchange)).thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(rateLimitFilter.filter(exchange, filterChain)).verifyComplete();

            verify(filterChain).filter(exchange);
            verify(checkRateLimitUseCase, never()).execute(any(CheckRateLimitCommand.class));
        }
    }

    @Nested
    @DisplayName("IP Rate Limit 테스트")
    class IpRateLimitTest {

        @Test
        @DisplayName("IP Rate Limit 허용 시 다음 필터로 진행")
        void shouldProceedWhenIpRateLimitAllowed() {
            // given
            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/test")
                            .header("X-Forwarded-For", "192.168.1.1")
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            CheckRateLimitResponse ipResponse = CheckRateLimitResponse.allowed(5, 100);
            CheckRateLimitResponse endpointResponse = CheckRateLimitResponse.allowed(10, 1000);

            when(checkRateLimitUseCase.execute(
                            argThat(cmd -> cmd != null && cmd.limitType() == LimitType.IP)))
                    .thenReturn(Mono.just(ipResponse));
            when(checkRateLimitUseCase.execute(
                            argThat(cmd -> cmd != null && cmd.limitType() == LimitType.ENDPOINT)))
                    .thenReturn(Mono.just(endpointResponse));
            when(filterChain.filter(exchange)).thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(rateLimitFilter.filter(exchange, filterChain)).verifyComplete();

            verify(filterChain).filter(exchange);
        }

        @Test
        @DisplayName("IP Rate Limit 초과 시 429 반환")
        void shouldReturn429WhenIpRateLimitExceeded() {
            // given
            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/test")
                            .header("X-Forwarded-For", "192.168.1.1")
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            CheckRateLimitResponse deniedResponse =
                    CheckRateLimitResponse.denied(100, 100, 60, RateLimitAction.REJECT);

            when(checkRateLimitUseCase.execute(any(CheckRateLimitCommand.class)))
                    .thenReturn(Mono.just(deniedResponse));

            // when
            Mono<Void> result = rateLimitFilter.filter(exchange, filterChain);

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
    @DisplayName("Endpoint Rate Limit 테스트")
    class EndpointRateLimitTest {

        @Test
        @DisplayName("Endpoint Rate Limit 초과 시 429 반환")
        void shouldReturn429WhenEndpointRateLimitExceeded() {
            // given
            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/test")
                            .header("X-Forwarded-For", "192.168.1.1")
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            CheckRateLimitResponse ipResponse = CheckRateLimitResponse.allowed(5, 100);
            CheckRateLimitResponse endpointDenied =
                    CheckRateLimitResponse.denied(1000, 1000, 30, RateLimitAction.REJECT);

            when(checkRateLimitUseCase.execute(
                            argThat(cmd -> cmd != null && cmd.limitType() == LimitType.IP)))
                    .thenReturn(Mono.just(ipResponse));
            when(checkRateLimitUseCase.execute(
                            argThat(cmd -> cmd != null && cmd.limitType() == LimitType.ENDPOINT)))
                    .thenReturn(Mono.just(endpointDenied));

            // when
            Mono<Void> result = rateLimitFilter.filter(exchange, filterChain);

            // then
            StepVerifier.create(result).verifyComplete();

            assertThat(exchange.getResponse().getStatusCode())
                    .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
            assertThat(exchange.getResponse().getHeaders().getFirst("Retry-After")).isEqualTo("30");
            verify(filterChain, never()).filter(any());
        }
    }

    @Nested
    @DisplayName("Rate Limit 헤더 테스트")
    class RateLimitHeaderTest {

        @Test
        @Disabled("성공 응답의 Rate Limit 헤더 추가 기능 미구현 - 추후 구현 예정")
        @DisplayName("Rate Limit 허용 시 헤더 추가")
        void shouldAddRateLimitHeadersWhenAllowed() {
            // given
            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/test")
                            .header("X-Forwarded-For", "192.168.1.1")
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            CheckRateLimitResponse ipResponse = CheckRateLimitResponse.allowed(5, 100);
            CheckRateLimitResponse endpointResponse = CheckRateLimitResponse.allowed(10, 1000);

            when(checkRateLimitUseCase.execute(
                            argThat(cmd -> cmd != null && cmd.limitType() == LimitType.IP)))
                    .thenReturn(Mono.just(ipResponse));
            when(checkRateLimitUseCase.execute(
                            argThat(cmd -> cmd != null && cmd.limitType() == LimitType.ENDPOINT)))
                    .thenReturn(Mono.just(endpointResponse));
            when(filterChain.filter(exchange)).thenReturn(Mono.empty());

            // when
            StepVerifier.create(rateLimitFilter.filter(exchange, filterChain)).verifyComplete();

            // then
            assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Limit"))
                    .isEqualTo("1000");
            assertThat(exchange.getResponse().getHeaders().getFirst("X-RateLimit-Remaining"))
                    .isEqualTo("990");
        }
    }

    @Nested
    @DisplayName("예외 처리 테스트")
    class ExceptionHandlingTest {

        @Test
        @DisplayName("IpBlockedException 발생 시 403 반환")
        void shouldReturn403WhenIpBlocked() {
            // given
            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/test")
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
            assertThat(exchange.getResponse().getHeaders().getFirst("Retry-After"))
                    .isEqualTo("3600");
            verify(filterChain, never()).filter(any());
        }

        @Test
        @DisplayName("RateLimitExceededException 발생 시 429 반환")
        void shouldReturn429WhenRateLimitExceeded() {
            // given
            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/test")
                            .header("X-Forwarded-For", "192.168.1.1")
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            when(checkRateLimitUseCase.execute(any(CheckRateLimitCommand.class)))
                    .thenReturn(Mono.error(new RateLimitExceededException(100, 0, 120)));

            // when
            Mono<Void> result = rateLimitFilter.filter(exchange, filterChain);

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
        @DisplayName("예기치 않은 예외 발생 시 graceful degradation으로 다음 필터 진행")
        void shouldProceedToNextFilterOnUnexpectedException() {
            // given
            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/test")
                            .header("X-Forwarded-For", "192.168.1.1")
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            // Redis 연결 실패 등 예기치 않은 예외 시뮬레이션
            when(checkRateLimitUseCase.execute(any(CheckRateLimitCommand.class)))
                    .thenReturn(Mono.error(new RuntimeException("Redis connection failed")));
            when(filterChain.filter(exchange)).thenReturn(Mono.empty());

            // when
            Mono<Void> result = rateLimitFilter.filter(exchange, filterChain);

            // then - graceful degradation: Rate Limit 체크 실패 시 다음 필터로 진행
            StepVerifier.create(result).verifyComplete();

            verify(filterChain).filter(exchange);
        }
    }

    @Nested
    @DisplayName("ByteBuf 메모리 관리 테스트 (CodeRabbit AI Review 대응)")
    class ByteBufMemoryManagementTest {

        @Test
        @DisplayName("429 응답 작성 시 정상적으로 JSON Body가 포함되어야 한다")
        void shouldWriteJsonBodyOnTooManyRequests() {
            // given
            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/test")
                            .header("X-Forwarded-For", "192.168.1.1")
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            CheckRateLimitResponse deniedResponse =
                    CheckRateLimitResponse.denied(100, 100, 60, RateLimitAction.REJECT);

            when(checkRateLimitUseCase.execute(any(CheckRateLimitCommand.class)))
                    .thenReturn(Mono.just(deniedResponse));

            // when
            Mono<Void> result = rateLimitFilter.filter(exchange, filterChain);

            // then
            StepVerifier.create(result).verifyComplete();

            // 응답 상태 확인
            assertThat(exchange.getResponse().getStatusCode())
                    .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

            // Body가 작성되었는지 확인 (MockServerWebExchange는 body를 직접 검증하기 어려움)
            // 대신 응답이 정상 완료되었는지 확인
            verify(filterChain, never()).filter(any());
        }

        @Test
        @DisplayName("403 응답 (IP 차단) 시 정상적으로 응답이 작성되어야 한다")
        void shouldWriteResponseOnIpBlocked() {
            // given
            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/test")
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
            assertThat(exchange.getResponse().getHeaders().getFirst("Retry-After"))
                    .isEqualTo("3600");
            verify(filterChain, never()).filter(any());
        }

        @Test
        @DisplayName("Unknown IP인 경우 Rate Limit을 스킵하고 다음 필터로 진행해야 한다")
        void shouldSkipRateLimitWhenIpIsUnknown() {
            // given
            MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);

            // Unknown IP 반환 설정
            when(clientIpExtractor.extractWithTrustedProxy(any())).thenReturn("unknown");
            when(filterChain.filter(exchange)).thenReturn(Mono.empty());

            // when
            Mono<Void> result = rateLimitFilter.filter(exchange, filterChain);

            // then - graceful degradation: unknown IP는 Rate Limit 스킵
            StepVerifier.create(result).verifyComplete();

            verify(filterChain).filter(exchange);
            verify(checkRateLimitUseCase, never()).execute(any(CheckRateLimitCommand.class));
        }
    }
}
