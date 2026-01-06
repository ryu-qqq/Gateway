package com.ryuqq.gateway.adapter.in.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ryuqq.gateway.adapter.in.gateway.common.util.ClientIpExtractor;
import com.ryuqq.gateway.adapter.in.gateway.config.PublicPathsProperties;
import com.ryuqq.gateway.application.authentication.port.in.command.ValidateJwtUseCase;
import com.ryuqq.gateway.application.ratelimit.port.in.command.RecordFailureUseCase;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
 * JwtAuthenticationFilter Host 인식 기능 테스트
 *
 * <p>Host 기반 서비스의 public-paths 분리 로직을 검증합니다.
 *
 * @author development-team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterHostAwareTest {

    @Mock private ValidateJwtUseCase validateJwtUseCase;

    @Mock private RecordFailureUseCase recordFailureUseCase;

    @Mock private GatewayFilterChain filterChain;

    @Mock private PublicPathsProperties publicPathsProperties;

    @Mock private ClientIpExtractor clientIpExtractor;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private final ObjectMapper objectMapper =
            new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        // 전역 public paths (host 기반 서비스 제외)
        lenient()
                .when(publicPathsProperties.getAllPublicPaths())
                .thenReturn(List.of("/actuator/**", "/api/v1/auth/login"));
        lenient().when(clientIpExtractor.extractWithTrustedProxy(any())).thenReturn("127.0.0.1");
        lenient().when(clientIpExtractor.extract(any())).thenReturn("127.0.0.1");

        jwtAuthenticationFilter =
                new JwtAuthenticationFilter(
                        validateJwtUseCase,
                        recordFailureUseCase,
                        objectMapper,
                        publicPathsProperties,
                        clientIpExtractor);
    }

    @Nested
    @DisplayName("전역 Public Path 테스트")
    class GlobalPublicPathTest {

        @Test
        @DisplayName("전역 public path는 JWT 검증 없이 통과해야 한다")
        void shouldBypassJwtValidationForGlobalPublicPath() {
            // given
            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/v1/auth/login")
                            .header(HttpHeaders.HOST, "api.set-of.com")
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            when(filterChain.filter(exchange)).thenReturn(Mono.empty());

            // when
            StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                    .verifyComplete();

            // then
            verify(validateJwtUseCase, never()).execute(any());
            verify(filterChain).filter(exchange);
        }
    }

    @Nested
    @DisplayName("Host 기반 Public Path 테스트")
    class HostBasedPublicPathTest {

        @Test
        @DisplayName("legacy host의 요청은 해당 host의 public-paths가 적용되어야 한다")
        void shouldApplyHostSpecificPublicPathsForLegacyHost() {
            // given
            when(publicPathsProperties.getPublicPathsForHost("stage.set-of.com"))
                    .thenReturn(List.of("/**"));

            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/v1/products/123")
                            .header(HttpHeaders.HOST, "stage.set-of.com")
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            when(filterChain.filter(exchange)).thenReturn(Mono.empty());

            // when
            StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                    .verifyComplete();

            // then - /** 패턴으로 인해 JWT 검증 없이 통과
            verify(validateJwtUseCase, never()).execute(any());
            verify(filterChain).filter(exchange);
        }

        @Test
        @DisplayName("api host의 요청은 JWT 검증이 필요해야 한다")
        void shouldRequireJwtValidationForApiHost() {
            // given
            when(publicPathsProperties.getPublicPathsForHost("api.set-of.com"))
                    .thenReturn(Collections.emptyList());

            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/v1/crawling/sellers/8")
                            .header(HttpHeaders.HOST, "api.set-of.com")
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            exchange.getAttributes().put(TraceIdFilter.TRACE_ID_ATTRIBUTE, "test-trace-id");

            // when - Authorization 헤더 없이 요청
            StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                    .verifyComplete();

            // then - JWT 없어서 401 반환
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            verify(filterChain, never()).filter(any());
        }
    }

    @Nested
    @DisplayName("X-Forwarded-Host 테스트")
    class XForwardedHostTest {

        @Test
        @DisplayName("X-Forwarded-Host가 있으면 해당 값을 사용해야 한다")
        void shouldUseXForwardedHostWhenPresent() {
            // given
            when(publicPathsProperties.getPublicPathsForHost("stage.set-of.com"))
                    .thenReturn(List.of("/**"));

            // Host 헤더는 internal ALB, X-Forwarded-Host는 원본 host
            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/v1/products/123")
                            .header(HttpHeaders.HOST, "internal-alb.local")
                            .header("X-Forwarded-Host", "stage.set-of.com")
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            when(filterChain.filter(exchange)).thenReturn(Mono.empty());

            // when
            StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                    .verifyComplete();

            // then - X-Forwarded-Host의 stage.set-of.com으로 판단하여 JWT 스킵
            verify(validateJwtUseCase, never()).execute(any());
            verify(filterChain).filter(exchange);
        }

        @Test
        @DisplayName("X-Forwarded-Host가 api host면 JWT 검증이 필요해야 한다")
        void shouldRequireJwtWhenXForwardedHostIsApiHost() {
            // given
            when(publicPathsProperties.getPublicPathsForHost("api.set-of.com"))
                    .thenReturn(Collections.emptyList());

            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/v1/crawling/sellers/8")
                            .header(HttpHeaders.HOST, "internal-alb.local")
                            .header("X-Forwarded-Host", "api.set-of.com")
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            exchange.getAttributes().put(TraceIdFilter.TRACE_ID_ATTRIBUTE, "test-trace-id");

            // when
            StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                    .verifyComplete();

            // then - api.set-of.com은 public paths가 없으므로 JWT 필요, 없어서 401
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("X-Forwarded-Host에 포트가 포함되어 있으면 포트를 제거해야 한다")
        void shouldRemovePortFromXForwardedHost() {
            // given
            when(publicPathsProperties.getPublicPathsForHost("stage.set-of.com"))
                    .thenReturn(List.of("/**"));

            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/v1/products/123")
                            .header(HttpHeaders.HOST, "internal-alb.local")
                            .header("X-Forwarded-Host", "stage.set-of.com:443")
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            when(filterChain.filter(exchange)).thenReturn(Mono.empty());

            // when
            StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                    .verifyComplete();

            // then - 포트 제거 후 stage.set-of.com으로 매칭되어 JWT 스킵
            verify(validateJwtUseCase, never()).execute(any());
            verify(filterChain).filter(exchange);
        }
    }

    @Nested
    @DisplayName("실제 시나리오 테스트")
    class RealScenarioTest {

        @Test
        @DisplayName("api.set-of.com에서 crawling API 호출 시 JWT 필요")
        void apiHostCrawlingEndpointShouldRequireJwt() {
            // given - 실제 prod 설정과 동일하게 설정
            when(publicPathsProperties.getPublicPathsForHost("api.set-of.com"))
                    .thenReturn(Collections.emptyList());

            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/v1/crawling/sellers/8")
                            .header("X-Forwarded-Host", "api.set-of.com")
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            exchange.getAttributes().put(TraceIdFilter.TRACE_ID_ATTRIBUTE, "test-trace-id");

            // when - JWT 없이 요청
            StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                    .verifyComplete();

            // then - 401 반환
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("stage.set-of.com에서 모든 경로는 public")
        void legacyHostAllPathsShouldBePublic() {
            // given - legacy-web의 /** public-paths
            when(publicPathsProperties.getPublicPathsForHost("stage.set-of.com"))
                    .thenReturn(List.of("/**"));

            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/v1/anything/any/path")
                            .header("X-Forwarded-Host", "stage.set-of.com")
                            .build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            when(filterChain.filter(exchange)).thenReturn(Mono.empty());

            // when
            StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                    .verifyComplete();

            // then - JWT 없이 통과
            verify(validateJwtUseCase, never()).execute(any());
            verify(filterChain).filter(exchange);
        }
    }
}
