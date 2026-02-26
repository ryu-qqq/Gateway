package com.ryuqq.gateway.adapter.in.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ryuqq.gateway.adapter.in.gateway.common.util.GatewayErrorResponder;
import com.ryuqq.gateway.adapter.in.gateway.config.GatewayFilterOrder;
import com.ryuqq.gateway.application.tenant.dto.query.GetTenantConfigQuery;
import com.ryuqq.gateway.application.tenant.dto.response.GetTenantConfigResponse;
import com.ryuqq.gateway.application.tenant.port.in.query.GetTenantConfigUseCase;
import com.ryuqq.gateway.domain.tenant.aggregate.TenantConfig;
import com.ryuqq.gateway.domain.tenant.id.TenantId;
import com.ryuqq.gateway.domain.tenant.vo.SessionConfig;
import com.ryuqq.gateway.domain.tenant.vo.SocialProvider;
import com.ryuqq.gateway.domain.tenant.vo.TenantRateLimitConfig;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
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
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * TenantIsolationFilter 테스트
 *
 * @author development-team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TenantIsolationFilter 테스트")
class TenantIsolationFilterTest {

    @Mock private GetTenantConfigUseCase getTenantConfigUseCase;

    @Mock private GatewayFilterChain filterChain;

    @Mock private GatewayErrorResponder errorResponder;

    private TenantIsolationFilter tenantIsolationFilter;

    @BeforeEach
    void setUp() {
        lenient()
                .when(errorResponder.internalServerError(any(), any(), any()))
                .thenAnswer(
                        invocation -> {
                            MockServerWebExchange exchange = invocation.getArgument(0);
                            exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                            return Mono.empty();
                        });
        tenantIsolationFilter = new TenantIsolationFilter(getTenantConfigUseCase, errorResponder);
    }

    /** 테스트용 TenantConfig 생성 */
    private TenantConfig createTestTenantConfig(String tenantIdStr, boolean mfaRequired) {
        TenantId tenantId = TenantId.from(tenantIdStr);
        SessionConfig sessionConfig =
                SessionConfig.of(5, Duration.ofMinutes(15), Duration.ofDays(7));
        TenantRateLimitConfig rateLimitConfig = TenantRateLimitConfig.of(10, 5);
        Set<SocialProvider> allowedSocialLogins =
                Set.of(SocialProvider.KAKAO, SocialProvider.GOOGLE);
        Map<String, Set<String>> roleHierarchy =
                Map.of(
                        "ADMIN", Set.of("READ", "WRITE", "DELETE"),
                        "USER", Set.of("READ"));

        return TenantConfig.of(
                tenantId,
                mfaRequired,
                allowedSocialLogins,
                roleHierarchy,
                sessionConfig,
                rateLimitConfig);
    }

    @Nested
    @DisplayName("Filter Order 테스트")
    class FilterOrderTest {

        @Test
        @DisplayName("Filter Order는 TENANT_ISOLATION_FILTER(5)여야 한다")
        void shouldHaveCorrectFilterOrder() {
            // when
            int order = tenantIsolationFilter.getOrder();

            // then
            assertThat(order).isEqualTo(GatewayFilterOrder.TENANT_ISOLATION_FILTER);
        }
    }

    @Nested
    @DisplayName("인증 컨텍스트 없을 때 테스트")
    class NoAuthContextTest {

        @Test
        @DisplayName("userId가 null이면 다음 필터로 통과")
        void shouldPassWhenUserIdIsNull() {
            // given
            MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            // userId, tenantId attribute 설정하지 않음

            when(filterChain.filter(exchange)).thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(tenantIsolationFilter.filter(exchange, filterChain))
                    .verifyComplete();

            verify(filterChain).filter(exchange);
            verify(getTenantConfigUseCase, never()).execute(any());
        }

        @Test
        @DisplayName("tenantId가 null이면 다음 필터로 통과")
        void shouldPassWhenTenantIdIsNull() {
            // given
            MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            exchange.getAttributes().put("userId", "user-123");
            // tenantId 설정하지 않음

            when(filterChain.filter(exchange)).thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(tenantIsolationFilter.filter(exchange, filterChain))
                    .verifyComplete();

            verify(filterChain).filter(exchange);
            verify(getTenantConfigUseCase, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("Tenant Config 로드 테스트")
    class TenantConfigLoadTest {

        @Test
        @DisplayName("Tenant Config 로드 성공 시 Exchange Attribute 설정")
        void shouldSetExchangeAttributesWhenConfigLoaded() {
            // given
            MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            exchange.getAttributes().put("userId", "user-123");
            exchange.getAttributes().put("tenantId", "tenant-123");
            exchange.getAttributes().put("roles", Set.of("ROLE_USER"));

            TenantConfig tenantConfig = createTestTenantConfig("tenant-123", true);
            GetTenantConfigResponse response = GetTenantConfigResponse.from(tenantConfig);

            when(getTenantConfigUseCase.execute(
                            argThat(
                                    query ->
                                            query != null
                                                    && "tenant-123".equals(query.tenantId()))))
                    .thenReturn(Mono.just(response));
            when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

            // when
            StepVerifier.create(tenantIsolationFilter.filter(exchange, filterChain))
                    .verifyComplete();

            // then
            assertThat((TenantConfig) exchange.getAttribute("tenantContext"))
                    .isEqualTo(tenantConfig);
            assertThat((Boolean) exchange.getAttribute("mfaRequired")).isTrue();
        }

        @Test
        @DisplayName("MFA 비활성화된 Tenant Config 로드")
        void shouldSetMfaRequiredFalseWhenDisabled() {
            // given
            MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            exchange.getAttributes().put("userId", "user-123");
            exchange.getAttributes().put("tenantId", "tenant-456");
            exchange.getAttributes().put("roles", Set.of("ROLE_USER"));

            TenantConfig tenantConfig = createTestTenantConfig("tenant-456", false);
            GetTenantConfigResponse response = GetTenantConfigResponse.from(tenantConfig);

            when(getTenantConfigUseCase.execute(any(GetTenantConfigQuery.class)))
                    .thenReturn(Mono.just(response));
            when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

            // when
            StepVerifier.create(tenantIsolationFilter.filter(exchange, filterChain))
                    .verifyComplete();

            // then
            assertThat((Boolean) exchange.getAttribute("mfaRequired")).isFalse();
        }
    }

    @Nested
    @DisplayName("예외 처리 테스트")
    class ExceptionHandlingTest {

        @Test
        @DisplayName("Tenant Config 로드 실패 시 500 반환")
        void shouldReturn500WhenConfigLoadFails() {
            // given
            MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            exchange.getAttributes().put("userId", "user-123");
            exchange.getAttributes().put("tenantId", "tenant-123");

            when(getTenantConfigUseCase.execute(any(GetTenantConfigQuery.class)))
                    .thenReturn(Mono.error(new RuntimeException("Redis connection failed")));

            // when
            Mono<Void> result = tenantIsolationFilter.filter(exchange, filterChain);

            // then
            StepVerifier.create(result).verifyComplete();

            assertThat(exchange.getResponse().getStatusCode())
                    .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            verify(filterChain, never()).filter(any());
        }
    }

    @Nested
    @DisplayName("Reactor Context 테스트")
    class ReactorContextTest {

        @Test
        @DisplayName("Reactor Context에 tenantId 저장")
        void shouldStoreTenantIdInReactorContext() {
            // given
            MockServerHttpRequest request = MockServerHttpRequest.get("/api/test").build();
            MockServerWebExchange exchange = MockServerWebExchange.from(request);
            exchange.getAttributes().put("userId", "user-123");
            exchange.getAttributes().put("tenantId", "tenant-123");
            exchange.getAttributes().put("roles", Set.of("ROLE_USER"));

            TenantConfig tenantConfig = createTestTenantConfig("tenant-123", false);
            GetTenantConfigResponse response = GetTenantConfigResponse.from(tenantConfig);

            when(getTenantConfigUseCase.execute(any(GetTenantConfigQuery.class)))
                    .thenReturn(Mono.just(response));

            // Reactor Context 검증을 위해 filterChain에서 context 확인
            when(filterChain.filter(any(ServerWebExchange.class)))
                    .thenReturn(
                            Mono.deferContextual(
                                    ctx -> {
                                        assertThat(ctx.hasKey("tenantId")).isTrue();
                                        String tenantIdFromContext = ctx.get("tenantId");
                                        assertThat(tenantIdFromContext).isEqualTo("tenant-123");
                                        return Mono.empty();
                                    }));

            // when & then
            StepVerifier.create(tenantIsolationFilter.filter(exchange, filterChain))
                    .verifyComplete();
        }
    }
}
