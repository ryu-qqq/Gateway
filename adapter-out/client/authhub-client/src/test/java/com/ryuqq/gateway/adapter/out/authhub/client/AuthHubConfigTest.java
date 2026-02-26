package com.ryuqq.gateway.adapter.out.authhub.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.ryuqq.gateway.adapter.out.authhub.client.config.AuthHubConfig;
import com.ryuqq.gateway.adapter.out.authhub.client.config.AuthHubProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * AuthHubConfig Unit Test
 *
 * <p>Configuration Bean 생성 검증
 *
 * @author development-team
 * @since 1.0.0
 */
@DisplayName("AuthHubConfig 단위 테스트")
class AuthHubConfigTest {

    private AuthHubProperties properties;
    private AuthHubConfig config;

    @BeforeEach
    void setUp() {
        properties = new AuthHubProperties();
        properties.setBaseUrl("http://localhost:9090");
        properties.getEndpoints().setJwks("/api/v1/auth/jwks");
        properties.getWebclient().setConnectionTimeout(3000);
        properties.getWebclient().setResponseTimeout(3000);
        properties.getRetry().setMaxAttempts(3);
        properties.getRetry().setWaitDuration(100);
        properties.getCircuitBreaker().setFailureRateThreshold(50);
        properties.getCircuitBreaker().setWaitDurationInOpenState(10000);
        properties.getCircuitBreaker().setSlidingWindowSize(10);
        properties.getCircuitBreaker().setMinimumNumberOfCalls(5);
        properties.getCircuitBreaker().setPermittedCallsInHalfOpen(3);

        config = new AuthHubConfig(properties);
    }

    @Nested
    @DisplayName("authHubWebClient Bean")
    class AuthHubWebClientTest {

        @Test
        @DisplayName("WebClient Bean이 생성된다")
        void shouldCreateWebClientBean() {
            // given
            WebClient.Builder builder = WebClient.builder();

            // when
            WebClient webClient = config.authHubWebClient(builder);

            // then
            assertThat(webClient).isNotNull();
        }
    }

    @Nested
    @DisplayName("retryRegistry Bean")
    class RetryRegistryTest {

        @Test
        @DisplayName("RetryRegistry Bean이 생성된다")
        void shouldCreateRetryRegistryBean() {
            // when
            RetryRegistry registry = config.retryRegistry();

            // then
            assertThat(registry).isNotNull();
        }

        @Test
        @DisplayName("RetryConfig가 properties 값으로 설정된다")
        void shouldConfigureRetryFromProperties() {
            // given
            properties.getRetry().setMaxAttempts(5);
            properties.getRetry().setWaitDuration(200);
            config = new AuthHubConfig(properties);

            // when
            RetryRegistry registry = config.retryRegistry();
            RetryConfig retryConfig = registry.getDefaultConfig();

            // then
            assertThat(retryConfig.getMaxAttempts()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("circuitBreakerRegistry Bean")
    class CircuitBreakerRegistryTest {

        @Test
        @DisplayName("CircuitBreakerRegistry Bean이 생성된다")
        void shouldCreateCircuitBreakerRegistryBean() {
            // when
            CircuitBreakerRegistry registry = config.circuitBreakerRegistry();

            // then
            assertThat(registry).isNotNull();
        }

        @Test
        @DisplayName("CircuitBreakerConfig가 properties 값으로 설정된다")
        void shouldConfigureCircuitBreakerFromProperties() {
            // given
            properties.getCircuitBreaker().setFailureRateThreshold(70);
            properties.getCircuitBreaker().setWaitDurationInOpenState(20000);
            properties.getCircuitBreaker().setSlidingWindowSize(20);
            properties.getCircuitBreaker().setMinimumNumberOfCalls(10);
            config = new AuthHubConfig(properties);

            // when
            CircuitBreakerRegistry registry = config.circuitBreakerRegistry();
            CircuitBreakerConfig cbConfig = registry.getDefaultConfig();

            // then
            assertThat(cbConfig.getFailureRateThreshold()).isEqualTo(70f);
            assertThat(cbConfig.getSlidingWindowSize()).isEqualTo(20);
            assertThat(cbConfig.getMinimumNumberOfCalls()).isEqualTo(10);
        }

        @Test
        @DisplayName("permittedNumberOfCallsInHalfOpenState가 3으로 설정된다")
        void shouldSetPermittedNumberOfCallsInHalfOpenState() {
            // when
            CircuitBreakerRegistry registry = config.circuitBreakerRegistry();
            CircuitBreakerConfig cbConfig = registry.getDefaultConfig();

            // then
            assertThat(cbConfig.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("생성자")
    class ConstructorTest {

        @Test
        @DisplayName("properties를 주입받아 생성된다")
        void shouldCreateWithProperties() {
            // when
            AuthHubConfig authHubConfig = new AuthHubConfig(properties);

            // then
            assertThat(authHubConfig).isNotNull();
        }
    }
}
