package com.ryuqq.gateway.adapter.in.gateway.metrics;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Gateway Metrics (Prometheus)
 *
 * <p>Gateway의 Rate Limit 및 보안 관련 메트릭을 Prometheus에 기록합니다.
 *
 * <p><strong>메트릭 목록</strong>:
 *
 * <ul>
 *   <li>gateway_rate_limit_exceeded_total - Rate Limit 초과 횟수
 *   <li>gateway_ip_blocked_total - IP 차단 횟수
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class GatewayMetrics {

    private static final String METRIC_RATE_LIMIT_EXCEEDED = "gateway_rate_limit_exceeded_total";
    private static final String METRIC_IP_BLOCKED = "gateway_ip_blocked_total";

    private static final String TAG_CLIENT_IP = "client_ip";
    private static final String TAG_METHOD = "method";
    private static final String TAG_PATH = "path";

    private final MeterRegistry meterRegistry;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "MeterRegistry is a Spring-managed singleton bean injected via DI")
    public GatewayMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Rate Limit 초과 메트릭 기록
     *
     * @param clientIp 클라이언트 IP
     * @param method HTTP 메서드
     * @param path 요청 경로
     */
    public void recordRateLimitExceeded(String clientIp, String method, String path) {
        Counter.builder(METRIC_RATE_LIMIT_EXCEEDED)
                .description("Rate limit exceeded count")
                .tag(TAG_CLIENT_IP, sanitizeIp(clientIp))
                .tag(TAG_METHOD, method)
                .tag(TAG_PATH, normalizePath(path))
                .register(meterRegistry)
                .increment();
    }

    /**
     * IP 차단 메트릭 기록
     *
     * @param clientIp 클라이언트 IP
     * @param method HTTP 메서드
     * @param path 요청 경로
     */
    public void recordIpBlocked(String clientIp, String method, String path) {
        Counter.builder(METRIC_IP_BLOCKED)
                .description("IP blocked count")
                .tag(TAG_CLIENT_IP, sanitizeIp(clientIp))
                .tag(TAG_METHOD, method)
                .tag(TAG_PATH, normalizePath(path))
                .register(meterRegistry)
                .increment();
    }

    /**
     * IP 주소 정규화 (메트릭 카디널리티 제한)
     *
     * <p>개별 IP를 모두 기록하면 메트릭 카디널리티가 폭발하므로, 서브넷 단위로 정규화합니다.
     *
     * @param ip 원본 IP
     * @return 정규화된 IP (IPv4: /24 서브넷, IPv6: 마스킹)
     */
    private String sanitizeIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return "unknown";
        }

        // IPv4: 마지막 옥텟을 0으로 마스킹 (/24 서브넷)
        if (ip.contains(".") && !ip.contains(":")) {
            int lastDot = ip.lastIndexOf('.');
            if (lastDot > 0) {
                return ip.substring(0, lastDot) + ".0";
            }
        }

        // IPv6: 전체 마스킹 (개인정보 보호)
        if (ip.contains(":")) {
            return "ipv6_masked";
        }

        return ip;
    }

    /**
     * 경로 정규화 (메트릭 카디널리티 제한)
     *
     * <p>경로 파라미터를 일반화하여 메트릭 카디널리티를 제한합니다.
     *
     * @param path 원본 경로
     * @return 정규화된 경로
     */
    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "unknown";
        }

        // UUID 패턴 정규화: /users/550e8400-... → /users/{id}
        String normalized =
                path.replaceAll(
                        "/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", "/{id}");

        // 숫자 ID 패턴 정규화: /users/123 → /users/{id}
        normalized = normalized.replaceAll("/\\d+", "/{id}");

        return normalized;
    }
}
