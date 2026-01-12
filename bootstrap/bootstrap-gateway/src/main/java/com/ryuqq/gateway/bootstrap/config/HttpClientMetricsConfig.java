package com.ryuqq.gateway.bootstrap.config;

import io.netty.channel.ChannelOption;
import java.net.URI;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

/**
 * HTTP Client 메트릭 활성화 설정
 *
 * <p>Spring Cloud Gateway의 Reactor Netty HTTP Client에 Micrometer 메트릭을 활성화합니다.
 *
 * <p><strong>활성화되는 메트릭:</strong>
 *
 * <ul>
 *   <li>reactor.netty.http.client.* - HTTP Client 요청/응답 메트릭
 *   <li>reactor_netty_connection_provider_* - Connection Pool 메트릭
 * </ul>
 *
 * <p><strong>Connection Pool 메트릭:</strong>
 *
 * <ul>
 *   <li>reactor_netty_connection_provider_active_connections - 활성 연결 수
 *   <li>reactor_netty_connection_provider_idle_connections - 유휴 연결 수
 *   <li>reactor_netty_connection_provider_pending_connections - 대기 중인 연결 요청 수
 *   <li>reactor_netty_connection_provider_max_connections - 최대 연결 수
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Configuration
public class HttpClientMetricsConfig {

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

    /**
     * Gateway용 HttpClient (Connection Pool 메트릭 활성화)
     *
     * <p>Spring Cloud Gateway의 기본 HttpClient를 대체하여 메트릭이 활성화된 ConnectionProvider를 사용합니다.
     *
     * <p>이 Bean은 {@code GatewayAutoConfiguration}의 {@code gatewayHttpClient}보다 우선 적용됩니다.
     *
     * @return HttpClient with metrics-enabled ConnectionProvider
     */
    @Bean
    public HttpClient gatewayHttpClient() {
        // 메트릭이 활성화된 ConnectionProvider 생성
        ConnectionProvider connectionProvider =
                ConnectionProvider.builder(poolName)
                        .maxConnections(maxConnections)
                        .pendingAcquireTimeout(Duration.ofMillis(acquireTimeout))
                        .maxIdleTime(maxIdleTime)
                        .metrics(true) // Connection Pool 메트릭 활성화
                        .build();

        // HttpClient 생성 (ConnectionProvider + 타임아웃 + 메트릭)
        return HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
                .responseTimeout(responseTimeout)
                .metrics(
                        true,
                        uriStr -> {
                            // URI에서 path만 추출하여 태그로 사용 (쿼리스트링 제거, 카디널리티 제어)
                            try {
                                String path = URI.create(uriStr).getPath();
                                if (path == null || path.isEmpty()) {
                                    return "/";
                                }
                                // path parameter를 일반화 (예: /api/v1/orders/123 ->
                                // /api/v1/orders/{id})
                                return normalizeUriPath(path);
                            } catch (Exception e) {
                                return "/unknown";
                            }
                        });
    }

    /**
     * URI Path 정규화
     *
     * <p>숫자로 된 path segment를 {id}로 치환하여 메트릭 카디널리티를 제어합니다.
     *
     * @param path 원본 URI path
     * @return 정규화된 path
     */
    private String normalizeUriPath(String path) {
        // 숫자로만 이루어진 path segment를 {id}로 치환
        // 예: /api/v1/orders/12345 -> /api/v1/orders/{id}
        return path.replaceAll("/\\d+", "/{id}");
    }
}
