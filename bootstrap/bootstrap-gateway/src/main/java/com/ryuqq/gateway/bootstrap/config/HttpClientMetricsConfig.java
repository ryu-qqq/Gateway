package com.ryuqq.gateway.bootstrap.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * Reactor Netty 메트릭을 Prometheus로 노출하기 위한 설정
 *
 * <p>Reactor Netty는 Micrometer의 Global Registry({@code Metrics.globalRegistry()})를 사용합니다. Spring
 * Boot의 MeterRegistry를 Global Registry에 추가하여 Prometheus로 메트릭이 노출되도록 합니다.
 *
 * <p><strong>중요:</strong> Connection Pool 메트릭은 Spring Cloud Gateway의 YAML 설정으로 활성화됩니다:
 *
 * <pre>{@code
 * spring.cloud.gateway.httpclient.pool.metrics: true
 * spring.cloud.gateway.httpclient.pool.name: gateway-pool
 * }</pre>
 *
 * <p><strong>활성화되는 메트릭:</strong>
 *
 * <ul>
 *   <li>reactor_netty_connection_provider_total_connections{name="gateway-pool"}
 *   <li>reactor_netty_connection_provider_active_connections{name="gateway-pool"}
 *   <li>reactor_netty_connection_provider_idle_connections{name="gateway-pool"}
 *   <li>reactor_netty_connection_provider_pending_connections{name="gateway-pool"}
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Configuration
public class HttpClientMetricsConfig {

    private static final Logger log = LoggerFactory.getLogger(HttpClientMetricsConfig.class);

    private final MeterRegistry meterRegistry;

    public HttpClientMetricsConfig(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Micrometer Global Registry에 Spring의 MeterRegistry 등록
     *
     * <p>Reactor Netty는 기본적으로 Micrometer의 Global Registry를 사용합니다. Spring Boot의 MeterRegistry를
     * Global Registry에 추가하여 Prometheus 엔드포인트(/actuator/prometheus)로 메트릭이 노출되도록 합니다.
     *
     * <p>이 작업은 애플리케이션 시작 시 한 번만 수행됩니다.
     */
    @PostConstruct
    public void registerMicrometerGlobalRegistry() {
        Metrics.addRegistry(meterRegistry);
        log.info(
                "Registered Spring MeterRegistry to Micrometer Global Registry for Reactor Netty"
                        + " metrics export to Prometheus");
    }
}
