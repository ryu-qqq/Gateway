package com.ryuqq.gateway.adapter.in.gateway.common.util;

import java.net.InetSocketAddress;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ServerWebExchange;

/**
 * 클라이언트 IP 추출 유틸리티
 *
 * <p>X-Forwarded-For 헤더 spoofing 방어를 위해 RemoteAddress를 기본으로 사용합니다.
 *
 * <p><strong>보안 고려사항</strong>:
 *
 * <ul>
 *   <li>X-Forwarded-For는 클라이언트가 조작 가능 → 직접 신뢰하지 않음
 *   <li>신뢰할 수 있는 프록시(Load Balancer, CDN) 뒤에서만 X-Forwarded-For 사용
 *   <li>프록시 없는 환경에서는 RemoteAddress만 사용
 * </ul>
 *
 * <p><strong>사용 모드</strong>:
 *
 * <ul>
 *   <li>Trusted Proxy Mode: 신뢰할 수 있는 프록시가 설정한 X-Forwarded-For 사용
 *   <li>Direct Mode: RemoteAddress만 사용 (기본값, 안전)
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public final class ClientIpExtractor {

    private static final Logger log = LoggerFactory.getLogger(ClientIpExtractor.class);

    private static final String X_FORWARDED_FOR_HEADER = "X-Forwarded-For";
    private static final String UNKNOWN_IP = "unknown";

    /** IPv4 주소 패턴 (간단한 검증용) */
    private static final Pattern IPV4_PATTERN =
            Pattern.compile("^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)(\\.(?!$)|$)){4}$");

    /** IPv6 주소 패턴 (간단한 검증용) */
    private static final Pattern IPV6_PATTERN =
            Pattern.compile("^([0-9a-fA-F]{0,4}:){2,7}[0-9a-fA-F]{0,4}$");

    private ClientIpExtractor() {
        // Utility class
    }

    /**
     * 클라이언트 IP 추출 (Direct Mode - 기본, 안전)
     *
     * <p>RemoteAddress만 사용합니다. X-Forwarded-For를 신뢰하지 않습니다.
     *
     * @param exchange ServerWebExchange
     * @return 클라이언트 IP (추출 불가 시 "unknown")
     */
    public static String extract(ServerWebExchange exchange) {
        return extractFromRemoteAddress(exchange);
    }

    /**
     * 클라이언트 IP 추출 (Trusted Proxy Mode)
     *
     * <p>신뢰할 수 있는 프록시(Load Balancer, CDN) 뒤에서만 사용하세요. X-Forwarded-For 헤더의 첫 번째 IP를 사용합니다.
     *
     * <p><strong>주의</strong>: 프록시 없는 환경에서 사용 시 IP spoofing 취약점이 발생합니다!
     *
     * @param exchange ServerWebExchange
     * @return 클라이언트 IP (추출 불가 시 "unknown")
     */
    public static String extractWithTrustedProxy(ServerWebExchange exchange) {
        String xForwardedFor = exchange.getRequest().getHeaders().getFirst(X_FORWARDED_FOR_HEADER);

        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            String firstIp = xForwardedFor.split(",")[0].trim();

            // IP 형식 검증 (spoofing 방어)
            if (isValidIpAddress(firstIp)) {
                return firstIp;
            }

            log.warn(
                    "Invalid IP format in X-Forwarded-For header: '{}'. Falling back to"
                            + " RemoteAddress.",
                    firstIp);
        }

        return extractFromRemoteAddress(exchange);
    }

    /**
     * RemoteAddress에서 IP 추출
     *
     * @param exchange ServerWebExchange
     * @return IP 주소 또는 "unknown"
     */
    private static String extractFromRemoteAddress(ServerWebExchange exchange) {
        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();

        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }

        log.warn("Unable to extract client IP from RemoteAddress. Returning 'unknown'.");
        return UNKNOWN_IP;
    }

    /**
     * IP 주소 형식 검증
     *
     * @param ip 검증할 IP 문자열
     * @return 유효한 IPv4 또는 IPv6 형식이면 true
     */
    private static boolean isValidIpAddress(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }

        return IPV4_PATTERN.matcher(ip).matches() || IPV6_PATTERN.matcher(ip).matches();
    }
}
