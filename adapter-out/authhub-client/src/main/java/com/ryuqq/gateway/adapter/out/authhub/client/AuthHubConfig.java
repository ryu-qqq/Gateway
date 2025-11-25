package com.ryuqq.gateway.adapter.out.authhub.client;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.netty.channel.ChannelOption;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * AuthHub Client Configuration
 *
 * <p>WebClient + Resilience4j (Retry, Circuit Breaker) 설정
 *
 * <p><strong>Retry 전략</strong>:
 *
 * <ul>
 *   <li>최대 3회 재시도 (authhub-client.yml 설정)
 *   <li>Exponential Backoff
 * </ul>
 *
 * <p><strong>Circuit Breaker 전략</strong>:
 *
 * <ul>
 *   <li>실패율 50% 초과 시 Open (authhub-client.yml 설정)
 *   <li>Half-Open 대기 시간: 10초
 *   <li>Closed 전환 임계값: 5번 연속 성공
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties(AuthHubProperties.class)
public class AuthHubConfig {

    private final AuthHubProperties properties;

    public AuthHubConfig(AuthHubProperties properties) {
        this.properties = properties;
    }

    /** WebClient Bean with connection timeout */
    @Bean
    public WebClient authHubWebClient(WebClient.Builder webClientBuilder) {
        HttpClient httpClient =
                HttpClient.create()
                        .option(
                                ChannelOption.CONNECT_TIMEOUT_MILLIS,
                                (int) properties.getTimeout().getConnection())
                        .responseTimeout(Duration.ofMillis(properties.getTimeout().getResponse()));

        return webClientBuilder
                .baseUrl(properties.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    /** Retry Configuration from properties (Exponential Backoff) */
    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig retryConfig =
                RetryConfig.custom()
                        .maxAttempts(properties.getRetry().getMaxAttempts())
                        .intervalFunction(
                                IntervalFunction.ofExponentialBackoff(
                                        properties.getRetry().getWaitDuration(), 2.0))
                        .build();

        return RetryRegistry.of(retryConfig);
    }

    /** Circuit Breaker Configuration from properties */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig circuitBreakerConfig =
                CircuitBreakerConfig.custom()
                        .failureRateThreshold(
                                properties.getCircuitBreaker().getFailureRateThreshold())
                        .waitDurationInOpenState(
                                Duration.ofMillis(
                                        properties
                                                .getCircuitBreaker()
                                                .getWaitDurationInOpenState()))
                        .slidingWindowSize(properties.getCircuitBreaker().getSlidingWindowSize())
                        .minimumNumberOfCalls(
                                properties.getCircuitBreaker().getMinimumNumberOfCalls())
                        .permittedNumberOfCallsInHalfOpenState(3)
                        .build();

        return CircuitBreakerRegistry.of(circuitBreakerConfig);
    }
}
