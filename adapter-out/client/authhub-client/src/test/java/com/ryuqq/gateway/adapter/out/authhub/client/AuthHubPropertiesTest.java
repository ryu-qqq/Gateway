package com.ryuqq.gateway.adapter.out.authhub.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.ryuqq.gateway.adapter.out.authhub.client.config.AuthHubProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * AuthHubProperties Unit Test
 *
 * <p>Configuration Properties 바인딩 검증
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("AuthHubProperties 단위 테스트")
class AuthHubPropertiesTest {

    private AuthHubProperties properties;

    @BeforeEach
    void setUp() {
        properties = new AuthHubProperties();
    }

    @Nested
    @DisplayName("기본 속성")
    class BasicPropertiesTest {

        @Test
        @DisplayName("baseUrl을 설정하고 조회한다")
        void shouldSetAndGetBaseUrl() {
            // given
            String baseUrl = "http://localhost:9090";

            // when
            properties.setBaseUrl(baseUrl);

            // then
            assertThat(properties.getBaseUrl()).isEqualTo(baseUrl);
        }

        @Test
        @DisplayName("jwksEndpoint 기본값은 /api/v1/auth/jwks 이다")
        void shouldHaveDefaultJwksEndpoint() {
            // then
            assertThat(properties.getJwksEndpoint()).isEqualTo("/api/v1/auth/jwks");
        }

        @Test
        @DisplayName("jwksEndpoint를 설정하고 조회한다")
        void shouldSetAndGetJwksEndpoint() {
            // given
            String endpoint = "/custom/jwks";

            // when
            properties.getEndpoints().setJwks(endpoint);

            // then
            assertThat(properties.getJwksEndpoint()).isEqualTo(endpoint);
        }
    }

    @Nested
    @DisplayName("Endpoints 설정")
    class EndpointsTest {

        @Test
        @DisplayName("기본 Endpoints 객체가 생성되어 있다")
        void shouldHaveDefaultEndpoints() {
            // then
            assertThat(properties.getEndpoints()).isNotNull();
        }

        @Test
        @DisplayName("모든 엔드포인트 기본값이 설정되어 있다")
        void shouldHaveDefaultEndpointValues() {
            // then
            assertThat(properties.getJwksEndpoint()).isEqualTo("/api/v1/auth/jwks");
            assertThat(properties.getRefreshEndpoint()).isEqualTo("/api/v1/auth/refresh");
            assertThat(properties.getTenantConfigEndpoint())
                    .isEqualTo("/api/v1/tenants/{tenantId}/config");
            assertThat(properties.getPermissionSpecEndpoint())
                    .isEqualTo("/api/v1/permissions/spec");
            assertThat(properties.getUserPermissionsEndpoint())
                    .isEqualTo("/api/v1/permissions/users/{userId}");
        }

        @Test
        @DisplayName("Endpoints 객체 전체를 교체한다")
        void shouldSetEndpoints() {
            // given
            AuthHubProperties.Endpoints newEndpoints = new AuthHubProperties.Endpoints();
            newEndpoints.setJwks("/custom/jwks");
            newEndpoints.setRefresh("/custom/refresh");

            // when
            properties.setEndpoints(newEndpoints);

            // then
            assertThat(properties.getJwksEndpoint()).isEqualTo("/custom/jwks");
            assertThat(properties.getRefreshEndpoint()).isEqualTo("/custom/refresh");
        }
    }

    @Nested
    @DisplayName("WebClient 설정")
    class WebClientConfigTest {

        @Test
        @DisplayName("기본 WebClient 객체가 생성되어 있다")
        void shouldHaveDefaultWebclient() {
            // then
            assertThat(properties.getWebclient()).isNotNull();
        }

        @Test
        @DisplayName("connectionTimeout 기본값은 3000ms 이다")
        void shouldHaveDefaultConnectionTimeout() {
            // then
            assertThat(properties.getWebclient().getConnectionTimeout()).isEqualTo(3000);
        }

        @Test
        @DisplayName("responseTimeout 기본값은 3000ms 이다")
        void shouldHaveDefaultResponseTimeout() {
            // then
            assertThat(properties.getWebclient().getResponseTimeout()).isEqualTo(3000);
        }

        @Test
        @DisplayName("maxConnections 기본값은 500이다")
        void shouldHaveDefaultMaxConnections() {
            // then
            assertThat(properties.getWebclient().getMaxConnections()).isEqualTo(500);
        }

        @Test
        @DisplayName("wireLoggingEnabled 기본값은 false이다")
        void shouldHaveDefaultWireLoggingEnabled() {
            // then
            assertThat(properties.getWebclient().isWireLoggingEnabled()).isFalse();
        }

        @Test
        @DisplayName("connectionTimeout을 설정하고 조회한다")
        void shouldSetAndGetConnectionTimeout() {
            // when
            properties.getWebclient().setConnectionTimeout(5000);

            // then
            assertThat(properties.getWebclient().getConnectionTimeout()).isEqualTo(5000);
        }

        @Test
        @DisplayName("responseTimeout을 설정하고 조회한다")
        void shouldSetAndGetResponseTimeout() {
            // when
            properties.getWebclient().setResponseTimeout(5000);

            // then
            assertThat(properties.getWebclient().getResponseTimeout()).isEqualTo(5000);
        }

        @Test
        @DisplayName("WebClient 객체 전체를 교체한다")
        void shouldSetWebclient() {
            // given
            AuthHubProperties.WebClientConfig newConfig = new AuthHubProperties.WebClientConfig();
            newConfig.setConnectionTimeout(10000);
            newConfig.setResponseTimeout(10000);
            newConfig.setMaxConnections(1000);
            newConfig.setWireLoggingEnabled(true);

            // when
            properties.setWebclient(newConfig);

            // then
            assertThat(properties.getWebclient().getConnectionTimeout()).isEqualTo(10000);
            assertThat(properties.getWebclient().getResponseTimeout()).isEqualTo(10000);
            assertThat(properties.getWebclient().getMaxConnections()).isEqualTo(1000);
            assertThat(properties.getWebclient().isWireLoggingEnabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("Retry 설정")
    class RetryTest {

        @Test
        @DisplayName("기본 Retry 객체가 생성되어 있다")
        void shouldHaveDefaultRetry() {
            // then
            assertThat(properties.getRetry()).isNotNull();
        }

        @Test
        @DisplayName("maxAttempts 기본값은 3이다")
        void shouldHaveDefaultMaxAttempts() {
            // then
            assertThat(properties.getRetry().getMaxAttempts()).isEqualTo(3);
        }

        @Test
        @DisplayName("waitDuration 기본값은 100ms 이다")
        void shouldHaveDefaultWaitDuration() {
            // then
            assertThat(properties.getRetry().getWaitDuration()).isEqualTo(100);
        }

        @Test
        @DisplayName("maxAttempts를 설정하고 조회한다")
        void shouldSetAndGetMaxAttempts() {
            // when
            properties.getRetry().setMaxAttempts(5);

            // then
            assertThat(properties.getRetry().getMaxAttempts()).isEqualTo(5);
        }

        @Test
        @DisplayName("waitDuration을 설정하고 조회한다")
        void shouldSetAndGetWaitDuration() {
            // when
            properties.getRetry().setWaitDuration(200);

            // then
            assertThat(properties.getRetry().getWaitDuration()).isEqualTo(200);
        }

        @Test
        @DisplayName("Retry 객체 전체를 교체한다")
        void shouldSetRetry() {
            // given
            AuthHubProperties.Retry newRetry = new AuthHubProperties.Retry();
            newRetry.setMaxAttempts(10);
            newRetry.setWaitDuration(500);

            // when
            properties.setRetry(newRetry);

            // then
            assertThat(properties.getRetry().getMaxAttempts()).isEqualTo(10);
            assertThat(properties.getRetry().getWaitDuration()).isEqualTo(500);
        }
    }

    @Nested
    @DisplayName("CircuitBreaker 설정")
    class CircuitBreakerTest {

        @Test
        @DisplayName("기본 CircuitBreaker 객체가 생성되어 있다")
        void shouldHaveDefaultCircuitBreaker() {
            // then
            assertThat(properties.getCircuitBreaker()).isNotNull();
        }

        @Test
        @DisplayName("failureRateThreshold 기본값은 50이다")
        void shouldHaveDefaultFailureRateThreshold() {
            // then
            assertThat(properties.getCircuitBreaker().getFailureRateThreshold()).isEqualTo(50);
        }

        @Test
        @DisplayName("waitDurationInOpenState 기본값은 10000ms 이다")
        void shouldHaveDefaultWaitDurationInOpenState() {
            // then
            assertThat(properties.getCircuitBreaker().getWaitDurationInOpenState())
                    .isEqualTo(10000);
        }

        @Test
        @DisplayName("slidingWindowSize 기본값은 10이다")
        void shouldHaveDefaultSlidingWindowSize() {
            // then
            assertThat(properties.getCircuitBreaker().getSlidingWindowSize()).isEqualTo(10);
        }

        @Test
        @DisplayName("minimumNumberOfCalls 기본값은 5이다")
        void shouldHaveDefaultMinimumNumberOfCalls() {
            // then
            assertThat(properties.getCircuitBreaker().getMinimumNumberOfCalls()).isEqualTo(5);
        }

        @Test
        @DisplayName("permittedCallsInHalfOpen 기본값은 3이다")
        void shouldHaveDefaultPermittedCallsInHalfOpen() {
            // then
            assertThat(properties.getCircuitBreaker().getPermittedCallsInHalfOpen()).isEqualTo(3);
        }

        @Test
        @DisplayName("failureRateThreshold를 설정하고 조회한다")
        void shouldSetAndGetFailureRateThreshold() {
            // when
            properties.getCircuitBreaker().setFailureRateThreshold(70);

            // then
            assertThat(properties.getCircuitBreaker().getFailureRateThreshold()).isEqualTo(70);
        }

        @Test
        @DisplayName("waitDurationInOpenState를 설정하고 조회한다")
        void shouldSetAndGetWaitDurationInOpenState() {
            // when
            properties.getCircuitBreaker().setWaitDurationInOpenState(20000);

            // then
            assertThat(properties.getCircuitBreaker().getWaitDurationInOpenState())
                    .isEqualTo(20000);
        }

        @Test
        @DisplayName("slidingWindowSize를 설정하고 조회한다")
        void shouldSetAndGetSlidingWindowSize() {
            // when
            properties.getCircuitBreaker().setSlidingWindowSize(20);

            // then
            assertThat(properties.getCircuitBreaker().getSlidingWindowSize()).isEqualTo(20);
        }

        @Test
        @DisplayName("minimumNumberOfCalls를 설정하고 조회한다")
        void shouldSetAndGetMinimumNumberOfCalls() {
            // when
            properties.getCircuitBreaker().setMinimumNumberOfCalls(10);

            // then
            assertThat(properties.getCircuitBreaker().getMinimumNumberOfCalls()).isEqualTo(10);
        }

        @Test
        @DisplayName("CircuitBreaker 객체 전체를 교체한다")
        void shouldSetCircuitBreaker() {
            // given
            AuthHubProperties.CircuitBreaker newCb = new AuthHubProperties.CircuitBreaker();
            newCb.setFailureRateThreshold(80);
            newCb.setWaitDurationInOpenState(30000);
            newCb.setSlidingWindowSize(30);
            newCb.setMinimumNumberOfCalls(15);
            newCb.setPermittedCallsInHalfOpen(5);

            // when
            properties.setCircuitBreaker(newCb);

            // then
            assertThat(properties.getCircuitBreaker().getFailureRateThreshold()).isEqualTo(80);
            assertThat(properties.getCircuitBreaker().getWaitDurationInOpenState())
                    .isEqualTo(30000);
            assertThat(properties.getCircuitBreaker().getSlidingWindowSize()).isEqualTo(30);
            assertThat(properties.getCircuitBreaker().getMinimumNumberOfCalls()).isEqualTo(15);
            assertThat(properties.getCircuitBreaker().getPermittedCallsInHalfOpen()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("중첩 클래스 인스턴스 생성")
    class NestedClassInstantiationTest {

        @Test
        @DisplayName("Endpoints 클래스를 직접 생성할 수 있다")
        void shouldCreateEndpointsInstance() {
            // when
            AuthHubProperties.Endpoints endpoints = new AuthHubProperties.Endpoints();

            // then
            assertThat(endpoints).isNotNull();
            assertThat(endpoints.getJwks()).isEqualTo("/api/v1/auth/jwks");
        }

        @Test
        @DisplayName("WebClientConfig 클래스를 직접 생성할 수 있다")
        void shouldCreateWebClientConfigInstance() {
            // when
            AuthHubProperties.WebClientConfig webclient = new AuthHubProperties.WebClientConfig();

            // then
            assertThat(webclient).isNotNull();
            assertThat(webclient.getConnectionTimeout()).isEqualTo(3000);
            assertThat(webclient.getResponseTimeout()).isEqualTo(3000);
            assertThat(webclient.getMaxConnections()).isEqualTo(500);
        }

        @Test
        @DisplayName("Retry 클래스를 직접 생성할 수 있다")
        void shouldCreateRetryInstance() {
            // when
            AuthHubProperties.Retry retry = new AuthHubProperties.Retry();

            // then
            assertThat(retry).isNotNull();
            assertThat(retry.getMaxAttempts()).isEqualTo(3);
            assertThat(retry.getWaitDuration()).isEqualTo(100);
        }

        @Test
        @DisplayName("CircuitBreaker 클래스를 직접 생성할 수 있다")
        void shouldCreateCircuitBreakerInstance() {
            // when
            AuthHubProperties.CircuitBreaker cb = new AuthHubProperties.CircuitBreaker();

            // then
            assertThat(cb).isNotNull();
            assertThat(cb.getFailureRateThreshold()).isEqualTo(50);
            assertThat(cb.getWaitDurationInOpenState()).isEqualTo(10000);
            assertThat(cb.getSlidingWindowSize()).isEqualTo(10);
            assertThat(cb.getMinimumNumberOfCalls()).isEqualTo(5);
            assertThat(cb.getPermittedCallsInHalfOpen()).isEqualTo(3);
        }
    }
}
