package com.ryuqq.gateway.adapter.out.authhub.client;

import static org.assertj.core.api.Assertions.assertThat;

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
            properties.setJwksEndpoint(endpoint);

            // then
            assertThat(properties.getJwksEndpoint()).isEqualTo(endpoint);
        }
    }

    @Nested
    @DisplayName("Timeout 설정")
    class TimeoutTest {

        @Test
        @DisplayName("기본 Timeout 객체가 생성되어 있다")
        void shouldHaveDefaultTimeout() {
            // then
            assertThat(properties.getTimeout()).isNotNull();
        }

        @Test
        @DisplayName("connection timeout 기본값은 3000ms 이다")
        void shouldHaveDefaultConnectionTimeout() {
            // then
            assertThat(properties.getTimeout().getConnection()).isEqualTo(3000);
        }

        @Test
        @DisplayName("response timeout 기본값은 3000ms 이다")
        void shouldHaveDefaultResponseTimeout() {
            // then
            assertThat(properties.getTimeout().getResponse()).isEqualTo(3000);
        }

        @Test
        @DisplayName("connection timeout을 설정하고 조회한다")
        void shouldSetAndGetConnectionTimeout() {
            // when
            properties.getTimeout().setConnection(5000);

            // then
            assertThat(properties.getTimeout().getConnection()).isEqualTo(5000);
        }

        @Test
        @DisplayName("response timeout을 설정하고 조회한다")
        void shouldSetAndGetResponseTimeout() {
            // when
            properties.getTimeout().setResponse(5000);

            // then
            assertThat(properties.getTimeout().getResponse()).isEqualTo(5000);
        }

        @Test
        @DisplayName("Timeout 객체 전체를 교체한다")
        void shouldSetTimeout() {
            // given
            AuthHubProperties.Timeout newTimeout = new AuthHubProperties.Timeout();
            newTimeout.setConnection(10000);
            newTimeout.setResponse(10000);

            // when
            properties.setTimeout(newTimeout);

            // then
            assertThat(properties.getTimeout().getConnection()).isEqualTo(10000);
            assertThat(properties.getTimeout().getResponse()).isEqualTo(10000);
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
            assertThat(properties.getCircuitBreaker().getWaitDurationInOpenState()).isEqualTo(10000);
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
            assertThat(properties.getCircuitBreaker().getWaitDurationInOpenState()).isEqualTo(20000);
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

            // when
            properties.setCircuitBreaker(newCb);

            // then
            assertThat(properties.getCircuitBreaker().getFailureRateThreshold()).isEqualTo(80);
            assertThat(properties.getCircuitBreaker().getWaitDurationInOpenState()).isEqualTo(30000);
            assertThat(properties.getCircuitBreaker().getSlidingWindowSize()).isEqualTo(30);
            assertThat(properties.getCircuitBreaker().getMinimumNumberOfCalls()).isEqualTo(15);
        }
    }

    @Nested
    @DisplayName("중첩 클래스 인스턴스 생성")
    class NestedClassInstantiationTest {

        @Test
        @DisplayName("Timeout 클래스를 직접 생성할 수 있다")
        void shouldCreateTimeoutInstance() {
            // when
            AuthHubProperties.Timeout timeout = new AuthHubProperties.Timeout();

            // then
            assertThat(timeout).isNotNull();
            assertThat(timeout.getConnection()).isEqualTo(3000);
            assertThat(timeout.getResponse()).isEqualTo(3000);
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
        }
    }
}
