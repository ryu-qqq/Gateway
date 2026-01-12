package com.ryuqq.gateway.bootstrap.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.netty.channel.ChannelOption;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

/**
 * Gateway HttpClient 설정 - Connection Pool 및 HTTP 메트릭 활성화
 *
 * <p>Spring Cloud Gateway의 기본 HttpClient를 대체하여 메트릭이 활성화된 HttpClient를 제공합니다. Spring Cloud Gateway는
 * {@code @ConditionalOnMissingBean}을 사용하므로, 이 Bean이 우선 적용됩니다.
 *
 * <p><strong>Connection Pool 메트릭 (reactor_netty_connection_provider_*):</strong>
 *
 * <ul>
 *   <li>active_connections - 활성 연결 수
 *   <li>idle_connections - 유휴 연결 수
 *   <li>pending_connections - 대기 중인 연결 요청 수
 *   <li>total_connections - 전체 연결 수
 *   <li>max_connections - 최대 연결 수
 * </ul>
 *
 * <p><strong>HTTP Client 메트릭 (reactor_netty_http_client_*):</strong>
 *
 * <ul>
 *   <li>data_received - 수신 데이터 바이트
 *   <li>data_sent - 송신 데이터 바이트
 *   <li>response_time - 응답 시간
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 * @see <a href="https://github.com/spring-cloud/spring-cloud-gateway/issues/2241">SCG Issue
 *     #2241</a>
 */
@Configuration
public class HttpClientMetricsConfig {

    private static final Logger log = LoggerFactory.getLogger(HttpClientMetricsConfig.class);

    private final MeterRegistry meterRegistry;

    @Value("${spring.cloud.gateway.httpclient.pool.max-connections:1000}")
    private int maxConnections;

    @Value("${spring.cloud.gateway.httpclient.pool.acquire-timeout:3000}")
    private int acquireTimeout;

    @Value("${spring.cloud.gateway.httpclient.pool.max-idle-time:20s}")
    private Duration maxIdleTime;

    @Value("${spring.cloud.gateway.httpclient.pool.name:gateway-pool}")
    private String poolName;

    @Value("${spring.cloud.gateway.httpclient.connect-timeout:3000}")
    private int connectTimeout;

    @Value("${spring.cloud.gateway.httpclient.response-timeout:30s}")
    private Duration responseTimeout;

    public HttpClientMetricsConfig(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Micrometer Global Registry에 Spring의 MeterRegistry 등록
     *
     * <p>Reactor Netty는 기본적으로 Micrometer의 Global Registry를 사용합니다. Spring Boot의 MeterRegistry를
     * Global Registry에 추가하여 Prometheus로 메트릭이 노출되도록 합니다.
     *
     * <p>중요: 이 작업은 ConnectionProvider/HttpClient 생성 전에 수행되어야 합니다.
     */
    @PostConstruct
    public void registerMicrometerGlobalRegistry() {
        Metrics.addRegistry(meterRegistry);
        log.info(
                "Registered Spring MeterRegistry to Micrometer Global Registry for Reactor Netty"
                        + " metrics");
    }

    /**
     * Gateway용 HttpClient Bean (메트릭 활성화)
     *
     * <p>Spring Cloud Gateway가 이 HttpClient를 사용하도록 Bean으로 등록합니다. {@code @ConditionalOnMissingBean}이
     * 적용되어 있으므로 우리 Bean이 GatewayAutoConfiguration의 기본 HttpClient를 대체합니다.
     *
     * <p>이 HttpClient는 내부적으로 메트릭이 활성화된 ConnectionProvider를 사용하므로, Connection Pool 메트릭이 정상적으로 수집됩니다.
     *
     * @return 메트릭이 활성화된 HttpClient
     */
    @Bean
    public HttpClient gatewayHttpClient() {
        // 1. Connection Pool 메트릭이 활성화된 ConnectionProvider 생성
        ConnectionProvider connectionProvider =
                ConnectionProvider.builder(poolName)
                        .maxConnections(maxConnections)
                        .pendingAcquireTimeout(Duration.ofMillis(acquireTimeout))
                        .maxIdleTime(maxIdleTime)
                        .metrics(true) // Connection Pool 메트릭 활성화
                        .build();

        log.info(
                "Created ConnectionProvider '{}' with metrics enabled: maxConnections={},"
                        + " acquireTimeout={}ms, maxIdleTime={}",
                poolName,
                maxConnections,
                acquireTimeout,
                maxIdleTime);

        // 2. HTTP 요청/응답 메트릭이 활성화된 HttpClient 생성
        HttpClient httpClient =
                HttpClient.create(connectionProvider)
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
                        .responseTimeout(responseTimeout)
                        .metrics(
                                true,
                                uriStr -> {
                                    try {
                                        String path = URI.create(uriStr).getPath();
                                        if (path == null || path.isEmpty()) {
                                            return "/";
                                        }
                                        // path parameter 일반화 (예: /api/v1/orders/123 ->
                                        // /api/v1/orders/{id})
                                        return path.replaceAll("/\\d+", "/{id}");
                                    } catch (Exception e) {
                                        return "/unknown";
                                    }
                                });

        log.info(
                "Created Gateway HttpClient with metrics enabled: connectTimeout={}ms,"
                        + " responseTimeout={}",
                connectTimeout,
                responseTimeout);

        return httpClient;
    }
}
