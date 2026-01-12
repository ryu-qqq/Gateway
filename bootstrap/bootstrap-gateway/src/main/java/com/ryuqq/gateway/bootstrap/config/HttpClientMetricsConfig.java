package com.ryuqq.gateway.bootstrap.config;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Reactor Netty 메트릭을 Prometheus로 노출하기 위한 설정
 *
 * <p>Reactor Netty는 Micrometer의 Global Registry를 사용합니다. Spring Boot의 MeterRegistry를 Global
 * Registry에 추가하여 Prometheus로 메트릭이 노출되도록 합니다.
 *
 * @author development-team
 * @since 1.0.0
 */
@Configuration
@EnableScheduling
public class HttpClientMetricsConfig {

    private static final Logger log = LoggerFactory.getLogger(HttpClientMetricsConfig.class);

    private final MeterRegistry meterRegistry;

    public HttpClientMetricsConfig(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Micrometer Global Registry에 Spring의 MeterRegistry 등록
     *
     * <p>Spring Boot 시작 시 가장 먼저 실행되어 Global Registry와 Spring MeterRegistry를 연결합니다.
     */
    @PostConstruct
    public void registerMicrometerGlobalRegistry() {
        Metrics.addRegistry(meterRegistry);
        log.info(
                "[METRICS] Registered Spring MeterRegistry to Micrometer Global Registry. "
                        + "Registry type: {}",
                meterRegistry.getClass().getSimpleName());
    }

    /**
     * 애플리케이션 시작 완료 후 Global Registry의 메트릭 목록 출력
     *
     * <p>디버깅 용도: reactor.netty 관련 메트릭이 등록되었는지 확인합니다.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void logMetricsOnStartup() {
        logReactorNettyMetrics("ApplicationReadyEvent");
    }

    /**
     * 주기적으로 Reactor Netty 메트릭 확인 (5분마다)
     *
     * <p>연결이 생성된 후에야 메트릭이 등록될 수 있으므로 주기적으로 확인합니다.
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 60000) // 1분 후 시작, 5분마다
    public void logMetricsPeriodically() {
        logReactorNettyMetrics("Scheduled check");
    }

    private void logReactorNettyMetrics(String trigger) {
        // Global Registry에서 reactor.netty 관련 메트릭 찾기
        List<String> globalReactorMetrics =
                Metrics.globalRegistry.getMeters().stream()
                        .map(Meter::getId)
                        .map(id -> id.getName())
                        .filter(name -> name.contains("reactor") || name.contains("netty"))
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList());

        // Spring MeterRegistry에서 reactor.netty 관련 메트릭 찾기
        List<String> springReactorMetrics =
                meterRegistry.getMeters().stream()
                        .map(Meter::getId)
                        .map(id -> id.getName())
                        .filter(name -> name.contains("reactor") || name.contains("netty"))
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList());

        log.info(
                "[METRICS] {} - Global Registry reactor/netty metrics ({}): {}",
                trigger,
                globalReactorMetrics.size(),
                globalReactorMetrics);

        log.info(
                "[METRICS] {} - Spring MeterRegistry reactor/netty metrics ({}): {}",
                trigger,
                springReactorMetrics.size(),
                springReactorMetrics);

        // connection.provider 메트릭이 있는지 구체적으로 확인
        List<String> connectionProviderMetrics =
                Metrics.globalRegistry.getMeters().stream()
                        .map(Meter::getId)
                        .map(id -> id.getName())
                        .filter(name -> name.contains("connection.provider"))
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList());

        if (connectionProviderMetrics.isEmpty()) {
            log.warn(
                    "[METRICS] {} - NO connection.provider metrics found! "
                            + "Check if spring.cloud.gateway.httpclient.pool.metrics=true is set.",
                    trigger);
        } else {
            log.info(
                    "[METRICS] {} - Connection Provider metrics found: {}",
                    trigger,
                    connectionProviderMetrics);
        }
    }
}
