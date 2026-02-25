package com.ryuqq.gateway.adapter.out.authhub.client;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.netty.channel.ChannelOption;
import io.netty.handler.logging.LogLevel;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

/**
 * AuthHub Client Configuration
 *
 * <p>WebClient + Resilience4j (Retry, Circuit Breaker) 설정
 *
 * <p><strong>WebClient 설정</strong>:
 *
 * <ul>
 *   <li>Connection Pool: 최대 연결 수, 대기 타임아웃, Idle 타임아웃
 *   <li>Timeout: Connection 타임아웃, Response 타임아웃
 *   <li>Logging: Wire 로깅 (환경별 on/off)
 *   <li>Error Handling: 4xx/5xx 에러 로깅
 * </ul>
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

    private static final Logger log = LoggerFactory.getLogger(AuthHubConfig.class);

    /** Maximum in-memory buffer size (2MB) */
    private static final int MAX_IN_MEMORY_SIZE = 2 * 1024 * 1024;

    private final AuthHubProperties properties;

    public AuthHubConfig(AuthHubProperties properties) {
        this.properties = properties;
    }

    /**
     * WebClient Bean with enhanced configuration
     *
     * <p>Features:
     *
     * <ul>
     *   <li>Connection Pool management
     *   <li>Wire logging (environment configurable)
     *   <li>Request/Response logging
     *   <li>Error handling
     * </ul>
     */
    @Bean
    public WebClient authHubWebClient(WebClient.Builder webClientBuilder) {
        AuthHubProperties.WebClientConfig webclientConfig = properties.getWebclient();

        // Connection Pool 설정
        ConnectionProvider connectionProvider =
                ConnectionProvider.builder("authhub-pool")
                        .maxConnections(webclientConfig.getMaxConnections())
                        .pendingAcquireTimeout(
                                Duration.ofMillis(webclientConfig.getPendingAcquireTimeout()))
                        .maxIdleTime(Duration.ofMillis(webclientConfig.getMaxIdleTime()))
                        .metrics(true)
                        .build();

        // HttpClient 설정
        HttpClient httpClient =
                HttpClient.create(connectionProvider)
                        .option(
                                ChannelOption.CONNECT_TIMEOUT_MILLIS,
                                (int) webclientConfig.getConnectionTimeout())
                        .responseTimeout(Duration.ofMillis(webclientConfig.getResponseTimeout()));

        // Wire 로깅 설정 (환경별 on/off)
        if (webclientConfig.isWireLoggingEnabled()) {
            httpClient =
                    httpClient.wiretap(
                            "reactor.netty.http.client.HttpClient",
                            LogLevel.DEBUG,
                            AdvancedByteBufFormat.TEXTUAL);
        }

        // Exchange Strategies (버퍼 크기 설정)
        ExchangeStrategies exchangeStrategies =
                ExchangeStrategies.builder()
                        .codecs(
                                configurer ->
                                        configurer
                                                .defaultCodecs()
                                                .maxInMemorySize(MAX_IN_MEMORY_SIZE))
                        .build();

        return webClientBuilder
                .baseUrl(properties.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(exchangeStrategies)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .filter(logRequest())
                .filter(logResponse())
                .build();
    }

    /**
     * Request 로깅 필터
     *
     * @return ExchangeFilterFunction
     */
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(
                clientRequest -> {
                    if (log.isDebugEnabled()) {
                        log.debug(
                                "AuthHub Request: {} {}",
                                clientRequest.method(),
                                clientRequest.url());
                    }
                    return Mono.just(clientRequest);
                });
    }

    /**
     * Response 로깅 필터
     *
     * @return ExchangeFilterFunction
     */
    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(
                clientResponse -> {
                    if (log.isDebugEnabled()) {
                        log.debug(
                                "AuthHub Response: {} {}",
                                clientResponse.statusCode().value(),
                                clientResponse.statusCode());
                    }
                    if (clientResponse.statusCode().isError()) {
                        log.warn(
                                "AuthHub Error Response: {} {}",
                                clientResponse.statusCode().value(),
                                clientResponse.statusCode());
                    }
                    return Mono.just(clientResponse);
                });
    }

    /**
     * Retry Configuration from properties (Exponential Backoff)
     *
     * @return RetryRegistry
     */
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

    /**
     * Circuit Breaker Configuration from properties
     *
     * @return CircuitBreakerRegistry
     */
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
                        .permittedNumberOfCallsInHalfOpenState(
                                properties.getCircuitBreaker().getPermittedCallsInHalfOpen())
                        .build();

        return CircuitBreakerRegistry.of(circuitBreakerConfig);
    }
}
