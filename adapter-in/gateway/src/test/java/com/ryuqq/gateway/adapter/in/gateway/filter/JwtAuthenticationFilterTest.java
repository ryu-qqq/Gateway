package com.ryuqq.gateway.adapter.in.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ryuqq.gateway.adapter.in.gateway.common.util.ClientIpExtractor;
import com.ryuqq.gateway.adapter.in.gateway.common.util.GatewayErrorResponder;
import com.ryuqq.gateway.adapter.in.gateway.config.GatewayFilterOrder;
import com.ryuqq.gateway.adapter.in.gateway.config.PublicPathsProperties;
import com.ryuqq.gateway.application.authentication.dto.command.ValidateJwtCommand;
import com.ryuqq.gateway.application.authentication.dto.response.ValidateJwtResponse;
import com.ryuqq.gateway.application.authentication.port.in.command.ValidateJwtUseCase;
import com.ryuqq.gateway.application.ratelimit.dto.command.RecordFailureCommand;
import com.ryuqq.gateway.application.ratelimit.port.in.command.RecordFailureUseCase;
import com.ryuqq.gateway.domain.authentication.vo.JwtClaims;
import java.time.Instant;
import java.util.List;
import java.util.Set;
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
import org.springframework.web.server.ServerWebExchange;
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

    @Mock private RecordFailureUseCase recordFailureUseCase;

    @Mock private GatewayFilterChain filterChain;

    @Mock private PublicPathsProperties publicPathsProperties;

    @Mock private ClientIpExtractor clientIpExtractor;

    @Mock private GatewayErrorResponder errorResponder;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    /** 테스트용 Public Paths */
    private static final List<String> TEST_PUBLIC_PATHS =
            List.of(
                    "/actuator/**",
                    "/api/v1/auth/login",
                    "/api/v1/auth/register",
                    "POST:/api/v1/market/seller-applications");

    @BeforeEach
    void setUp() {
        lenient().when(publicPathsProperties.getAllPublicPaths()).thenReturn(TEST_PUBLIC_PATHS);
        lenient().when(clientIpExtractor.extractWithTrustedProxy(any())).thenReturn("127.0.0.1");
        lenient().when(clientIpExtractor.extract(any())).thenReturn("127.0.0.1");
        lenient()
                .when(errorResponder.unauthorized(any(), any(), any()))
                .thenAnswer(
                        invocation -> {
                            MockServerWebExchange exchange = invocation.getArgument(0);
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return Mono.empty();
                        });
        jwtAuthenticationFilter =
                new JwtAuthenticationFilter(
                        validateJwtUseCase,
                        recordFailureUseCase,
                        publicPathsProperties,
                        clientIpExtractor,
                        errorResponder);
    }

    /** recordFailureUseCase 기본 동작 설정 - 실패 기록이 필요한 테스트에서만 호출 */
    private void setupRecordFailureStub() {
        when(recordFailureUseCase.execute(any(RecordFailureCommand.class)))
                .thenReturn(Mono.empty());
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

        // ConcurrentHashMap은 null 값을 허용하지 않으므로 tenantId, organizationId, permissionHash 제공
        JwtClaims claims =
                JwtClaims.of(
                        "user-123",
                        "auth-hub",
                        Instant.now().plusSeconds(3600),
                        Instant.now(),
                        List.of("ROLE_USER"),
                        List.of("order:read"),
                        "tenant-123",
                        "org-789",
                        "hash-456",
                        false);
        ValidateJwtResponse response = new ValidateJwtResponse(claims, true);
        when(validateJwtUseCase.execute(any(ValidateJwtCommand.class)))
                .thenReturn(Mono.just(response));
        // Filter가 mutatedExchange로 chain을 호출하므로 any()로 매칭
        when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // when & then
        StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain)).verifyComplete();
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 401을 반환해야 한다 (실패 기록 없이)")
    void shouldReturn401WhenAuthorizationHeaderMissing() {
        // given
        // Authorization 헤더가 없는 경우는 Invalid JWT 공격이 아니므로 recordFailure 호출되지 않음
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getAttributes().put(TraceIdFilter.TRACE_ID_ATTRIBUTE, "test-trace-id");

        // when
        Mono<Void> result = jwtAuthenticationFilter.filter(exchange, filterChain);

        // then
        StepVerifier.create(result).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(recordFailureUseCase, never()).execute(any());
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

        // ConcurrentHashMap은 null 값을 허용하지 않으므로 tenantId, organizationId, permissionHash 제공
        JwtClaims claims =
                JwtClaims.of(
                        subject,
                        "auth-hub",
                        Instant.now().plusSeconds(3600),
                        Instant.now(),
                        roles,
                        List.of("order:read", "order:write"),
                        "tenant-123",
                        "org-789",
                        "hash-456",
                        false);
        ValidateJwtResponse response = new ValidateJwtResponse(claims, true);
        when(validateJwtUseCase.execute(any(ValidateJwtCommand.class)))
                .thenReturn(Mono.just(response));
        // Filter가 mutatedExchange로 chain을 호출하므로 any()로 매칭
        when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // when
        StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain)).verifyComplete();

        // then
        assertThat((String) exchange.getAttribute("userId")).isEqualTo(subject);
        // Filter는 roles를 Set<String>으로 저장함
        @SuppressWarnings("unchecked")
        Set<String> actualRoles = (Set<String>) exchange.getAttribute("roles");
        assertThat(actualRoles).containsExactlyInAnyOrderElementsOf(roles);
    }

    @Test
    @DisplayName("JWT 검증 실패 시 401을 반환해야 한다")
    void shouldReturn401WhenJwtValidationFails() {
        // given
        setupRecordFailureStub();
        String token = "invalid-token";
        MockServerHttpRequest request =
                MockServerHttpRequest.get("/api/test")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        exchange.getAttributes().put(TraceIdFilter.TRACE_ID_ATTRIBUTE, "test-trace-id");

        ValidateJwtResponse failedResponse = new ValidateJwtResponse(null, false);
        when(validateJwtUseCase.execute(any(ValidateJwtCommand.class)))
                .thenReturn(Mono.just(failedResponse));

        // when
        Mono<Void> result = jwtAuthenticationFilter.filter(exchange, filterChain);

        // then
        StepVerifier.create(result).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Nested
    @DisplayName("Method-aware Public Paths 테스트")
    class MethodAwarePublicPathsTest {

        @Test
        @DisplayName("POST:/path 패턴은 POST 요청에서 JWT 검증을 스킵해야 한다")
        void shouldSkipJwtForPostWhenMethodPrefixIsPost() {
            // given
            MockServerHttpRequest request =
                    MockServerHttpRequest.post("/api/v1/market/seller-applications").build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                    .verifyComplete();

            verify(filterChain).filter(any(ServerWebExchange.class));
            verify(validateJwtUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("POST:/path 패턴은 GET 요청에서 JWT 검증을 수행해야 한다")
        void shouldRequireJwtForGetWhenMethodPrefixIsPost() {
            // given
            MockServerHttpRequest request =
                    MockServerHttpRequest.get("/api/v1/market/seller-applications").build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            exchange.getAttributes().put(TraceIdFilter.TRACE_ID_ATTRIBUTE, "test-trace-id");

            // when & then (토큰 없으므로 401)
            StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                    .verifyComplete();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("메서드 접두사 없는 패턴은 모든 메서드에서 JWT 검증을 스킵해야 한다")
        void shouldSkipJwtForAllMethodsWhenNoMethodPrefix() {
            // given
            MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/auth/login").build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(jwtAuthenticationFilter.filter(exchange, filterChain))
                    .verifyComplete();

            verify(filterChain).filter(any(ServerWebExchange.class));
            verify(validateJwtUseCase, never()).execute(any());
        }
    }
}
