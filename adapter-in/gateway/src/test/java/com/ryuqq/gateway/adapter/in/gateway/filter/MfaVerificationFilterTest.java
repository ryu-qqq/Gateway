package com.ryuqq.gateway.adapter.in.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ryuqq.gateway.adapter.in.gateway.common.util.GatewayErrorResponder;
import com.ryuqq.gateway.adapter.in.gateway.config.GatewayFilterOrder;
import com.ryuqq.gateway.domain.tenant.TenantConfig;
import com.ryuqq.gateway.domain.tenant.vo.SessionConfig;
import com.ryuqq.gateway.domain.tenant.vo.TenantId;
import com.ryuqq.gateway.domain.tenant.vo.TenantRateLimitConfig;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@DisplayName("MfaVerificationFilter 테스트")
class MfaVerificationFilterTest {

    @Mock private GatewayFilterChain chain;

    @Mock private GatewayErrorResponder errorResponder;

    private MfaVerificationFilter mfaVerificationFilter;

    @BeforeEach
    void setUp() {
        lenient()
                .when(errorResponder.forbidden(any(), any(), any()))
                .thenAnswer(
                        invocation -> {
                            MockServerWebExchange exchange = invocation.getArgument(0);
                            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                            return Mono.empty();
                        });
        mfaVerificationFilter = new MfaVerificationFilter(errorResponder);
    }

    private TenantConfig createTenantConfig(String tenantId, boolean mfaRequired) {
        return TenantConfig.of(
                TenantId.of(tenantId),
                mfaRequired,
                Set.of(),
                Map.of("USER", Set.of("READ")),
                SessionConfig.defaultConfig(),
                TenantRateLimitConfig.defaultConfig());
    }

    @Nested
    @DisplayName("getOrder() 테스트")
    class GetOrderTest {

        @Test
        @DisplayName("올바른 필터 순서 반환")
        void shouldReturnCorrectFilterOrder() {
            // when
            int order = mfaVerificationFilter.getOrder();

            // then
            assertThat(order).isEqualTo(GatewayFilterOrder.MFA_VERIFICATION_FILTER);
        }
    }

    @Nested
    @DisplayName("filter() 테스트")
    class FilterTest {

        @Test
        @DisplayName("Tenant Context가 없으면 MFA 검증 스킵")
        void shouldSkipMfaVerificationWhenNoTenantContext() {
            // given
            ServerWebExchange exchange = createExchange("/api/v1/public", HttpMethod.GET);
            // tenantContext 설정하지 않음
            when(chain.filter(exchange)).thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(mfaVerificationFilter.filter(exchange, chain)).verifyComplete();

            verify(chain).filter(exchange);
        }

        @Test
        @DisplayName("MFA 필수가 아닌 테넌트는 MFA 검증 스킵")
        void shouldSkipMfaVerificationWhenMfaNotRequired() {
            // given
            ServerWebExchange exchange = createExchange("/api/v1/users", HttpMethod.GET);
            TenantConfig tenantConfig = createTenantConfig("tenant-1", false);
            exchange.getAttributes().put("tenantContext", tenantConfig);
            when(chain.filter(exchange)).thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(mfaVerificationFilter.filter(exchange, chain)).verifyComplete();

            verify(chain).filter(exchange);
        }

        @Test
        @DisplayName("MFA 필수 테넌트에서 MFA 검증 완료 시 통과")
        void shouldPassWhenMfaVerifiedForMfaRequiredTenant() {
            // given
            ServerWebExchange exchange = createExchange("/api/v1/secure", HttpMethod.GET);
            TenantConfig tenantConfig = createTenantConfig("tenant-2", true);
            exchange.getAttributes().put("tenantContext", tenantConfig);
            exchange.getAttributes().put("mfaVerified", true);
            exchange.getAttributes().put("userId", "user123");
            when(chain.filter(exchange)).thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(mfaVerificationFilter.filter(exchange, chain)).verifyComplete();

            verify(chain).filter(exchange);
        }

        @Test
        @DisplayName("MFA 필수 테넌트에서 MFA 미검증 시 403 Forbidden 응답")
        void shouldReturnForbiddenWhenMfaNotVerified() {
            // given
            ServerWebExchange exchange = createExchange("/api/v1/secure", HttpMethod.GET);
            TenantConfig tenantConfig = createTenantConfig("tenant-2", true);
            exchange.getAttributes().put("tenantContext", tenantConfig);
            exchange.getAttributes().put("mfaVerified", false);
            exchange.getAttributes().put("userId", "user123");

            // when & then
            StepVerifier.create(mfaVerificationFilter.filter(exchange, chain)).verifyComplete();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("MFA 필수 테넌트에서 mfaVerified 속성이 null인 경우 403 Forbidden 응답")
        void shouldReturnForbiddenWhenMfaVerifiedAttributeIsNull() {
            // given
            ServerWebExchange exchange = createExchange("/api/v1/secure", HttpMethod.POST);
            TenantConfig tenantConfig = createTenantConfig("tenant-3", true);
            exchange.getAttributes().put("tenantContext", tenantConfig);
            exchange.getAttributes().put("userId", "user456");
            // mfaVerified 속성을 설정하지 않음 (null)

            // when & then
            StepVerifier.create(mfaVerificationFilter.filter(exchange, chain)).verifyComplete();

            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("다양한 HTTP 메서드에서 MFA 검증 동작")
        void shouldWorkWithVariousHttpMethods() {
            // given
            ServerWebExchange exchange = createExchange("/api/v1/data", HttpMethod.DELETE);
            TenantConfig tenantConfig = createTenantConfig("tenant-4", true);
            exchange.getAttributes().put("tenantContext", tenantConfig);
            exchange.getAttributes().put("mfaVerified", true);
            exchange.getAttributes().put("userId", "admin");
            when(chain.filter(exchange)).thenReturn(Mono.empty());

            // when & then
            StepVerifier.create(mfaVerificationFilter.filter(exchange, chain)).verifyComplete();

            verify(chain).filter(exchange);
        }
    }

    @Nested
    @DisplayName("에러 응답 테스트")
    class ErrorResponseTest {

        @Test
        @DisplayName("forbidden 응답에 올바른 상태 코드 반환")
        void shouldIncludeCorrectStatusCodeInForbiddenResponse() {
            // given
            ServerWebExchange exchange = createExchange("/api/v1/secure", HttpMethod.GET);
            TenantConfig tenantConfig = createTenantConfig("tenant-5", true);
            exchange.getAttributes().put("tenantContext", tenantConfig);
            exchange.getAttributes().put("mfaVerified", false);

            // when
            StepVerifier.create(mfaVerificationFilter.filter(exchange, chain)).verifyComplete();

            // then
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    // Helper methods
    private ServerWebExchange createExchange(String path, HttpMethod method) {
        MockServerHttpRequest request = MockServerHttpRequest.method(method, path).build();
        return MockServerWebExchange.from(request);
    }
}
