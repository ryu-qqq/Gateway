package com.ryuqq.gateway.adapter.in.gateway.metrics;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Gateway 메트릭 관리
 *
 * <p>Rate Limit, IP 블락, 404 등의 메트릭을 Prometheus로 수집합니다.
 *
 * <p><strong>메트릭 목록</strong>:
 *
 * <ul>
 *   <li>gateway_rate_limit_exceeded_total - Rate Limit 초과 횟수
 *   <li>gateway_ip_blocked_total - IP 블락 횟수
 *   <li>gateway_not_found_total - 404 응답 횟수
 * </ul>
 *
 * <p><strong>Cardinality 방지</strong>:
 *
 * <ul>
 *   <li>IP 태그 제거 - IP별 메트릭 수집 시 OOM 위험
 *   <li>경로 화이트리스트 - 알려진 API 패턴만 허용, 나머지는 "other"
 *   <li>UUID/숫자 정규화 - 동적 세그먼트를 {id}로 치환
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class GatewayMetrics {

    private static final String METRIC_PREFIX = "gateway";

    /** 알려진 API 경로 패턴 (화이트리스트) */
    private static final Set<String> KNOWN_PATH_PREFIXES =
            Set.of("/api/", "/actuator/", "/oauth2/", "/login", "/logout", "/health", "/admin/");

    /** 숫자로만 구성된 경로 세그먼트 패턴 */
    private static final Pattern NUMERIC_SEGMENT = Pattern.compile("/\\d+");

    /** UUID 패턴 (8-4-4-4-12 형식) */
    private static final Pattern UUID_SEGMENT =
            Pattern.compile(
                    "/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    /** 긴 영숫자 문자열 (토큰, 해시 등) */
    private static final Pattern LONG_ALPHANUMERIC = Pattern.compile("/[a-zA-Z0-9]{20,}");

    private final MeterRegistry meterRegistry;

    /** Counter 캐시 (태그 조합별) - 핫패스에서 매번 Counter.builder() 호출 방지 */
    private final ConcurrentMap<String, Counter> counterCache = new ConcurrentHashMap<>();

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "MeterRegistry is a Spring-managed singleton bean injected via DI")
    public GatewayMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Rate Limit 초과 메트릭 기록
     *
     * @param method HTTP 메서드
     * @param path 요청 경로
     */
    public void recordRateLimitExceeded(String method, String path) {
        String sanitizedMethod = sanitizeMethod(method);
        String normalizedPath = normalizePath(path);
        String cacheKey = "rate_limit:" + sanitizedMethod + ":" + normalizedPath;

        counterCache
                .computeIfAbsent(
                        cacheKey,
                        key ->
                                Counter.builder(METRIC_PREFIX + "_rate_limit_exceeded_total")
                                        .description("Rate limit exceeded count")
                                        .tag("method", sanitizedMethod)
                                        .tag("path", normalizedPath)
                                        .register(meterRegistry))
                .increment();
    }

    /**
     * IP 블락 메트릭 기록
     *
     * @param method HTTP 메서드
     * @param path 요청 경로
     */
    public void recordIpBlocked(String method, String path) {
        String sanitizedMethod = sanitizeMethod(method);
        String normalizedPath = normalizePath(path);
        String cacheKey = "ip_blocked:" + sanitizedMethod + ":" + normalizedPath;

        counterCache
                .computeIfAbsent(
                        cacheKey,
                        key ->
                                Counter.builder(METRIC_PREFIX + "_ip_blocked_total")
                                        .description("IP blocked count")
                                        .tag("method", sanitizedMethod)
                                        .tag("path", normalizedPath)
                                        .register(meterRegistry))
                .increment();
    }

    /**
     * 404 Not Found 메트릭 기록
     *
     * @param method HTTP 메서드
     * @param path 요청 경로
     * @param suspicious 의심스러운 요청 여부
     */
    public void recordNotFound(String method, String path, boolean suspicious) {
        String sanitizedMethod = sanitizeMethod(method);
        String normalizedPath = normalizePath(path);
        String suspiciousStr = String.valueOf(suspicious);
        String cacheKey =
                "not_found:" + sanitizedMethod + ":" + normalizedPath + ":" + suspiciousStr;

        counterCache
                .computeIfAbsent(
                        cacheKey,
                        key ->
                                Counter.builder(METRIC_PREFIX + "_not_found_total")
                                        .description("Not found (404) count")
                                        .tag("method", sanitizedMethod)
                                        .tag("path", normalizedPath)
                                        .tag("suspicious", suspiciousStr)
                                        .register(meterRegistry))
                .increment();
    }

    /**
     * HTTP 메서드 정규화
     *
     * <p>알려진 메서드만 허용, 나머지는 "OTHER"
     */
    private String sanitizeMethod(String method) {
        if (method == null) {
            return "UNKNOWN";
        }
        String upper = method.toUpperCase();
        return switch (upper) {
            case "GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS" -> upper;
            default -> "OTHER";
        };
    }

    /**
     * 경로 정규화 (카디널리티 폭발 방지)
     *
     * <p>1. 알려진 경로 패턴만 화이트리스트로 허용 2. 동적 세그먼트(숫자, UUID, 긴 문자열)를 {id}로 치환 3. 알 수 없는 경로는 "other"로 처리
     */
    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "unknown";
        }

        // 1. 화이트리스트 체크
        boolean isKnownPath =
                KNOWN_PATH_PREFIXES.stream()
                        .anyMatch(
                                prefix -> {
                                    // trailing slash 제거 후 비교 (예: "/api/" -> "/api")
                                    String normalizedPrefix =
                                            prefix.endsWith("/")
                                                    ? prefix.substring(0, prefix.length() - 1)
                                                    : prefix;
                                    return path.startsWith(normalizedPrefix);
                                });

        if (!isKnownPath) {
            // 알려지지 않은 경로 (봇 공격, 랜덤 경로 등) → "other"
            return "other";
        }

        // 2. 동적 세그먼트 정규화
        String normalized = path;

        // UUID 형식 치환
        normalized = UUID_SEGMENT.matcher(normalized).replaceAll("/{id}");

        // 숫자로만 구성된 세그먼트 치환
        normalized = NUMERIC_SEGMENT.matcher(normalized).replaceAll("/{id}");

        // 긴 영숫자 문자열 치환 (토큰, 해시 등)
        normalized = LONG_ALPHANUMERIC.matcher(normalized).replaceAll("/{id}");

        // 3. 경로 길이 제한 (혹시 모를 긴 경로)
        if (normalized.length() > 100) {
            int thirdSlash = findNthOccurrence(normalized, '/', 3);
            if (thirdSlash > 0) {
                return normalized.substring(0, thirdSlash) + "/...";
            }
            return normalized.substring(0, 100) + "...";
        }

        return normalized;
    }

    private int findNthOccurrence(String str, char ch, int n) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == ch) {
                count++;
                if (count == n) {
                    return i;
                }
            }
        }
        return -1;
    }
}
