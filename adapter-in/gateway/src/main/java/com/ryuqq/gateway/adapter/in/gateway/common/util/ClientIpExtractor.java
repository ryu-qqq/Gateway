package com.ryuqq.gateway.adapter.in.gateway.common.util;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
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
@Component
public class ClientIpExtractor {

    private static final Logger log = LoggerFactory.getLogger(ClientIpExtractor.class);

    private static final String X_FORWARDED_FOR_HEADER = "X-Forwarded-For";
    public static final String UNKNOWN_IP = "unknown";

    /**
     * 클라이언트 IP 추출 (Direct Mode - 기본, 안전)
     *
     * <p>RemoteAddress만 사용합니다. X-Forwarded-For를 신뢰하지 않습니다.
     *
     * @param exchange ServerWebExchange
     * @return 클라이언트 IP (추출 불가 시 "unknown")
     */
    public String extract(ServerWebExchange exchange) {
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
    public String extractWithTrustedProxy(ServerWebExchange exchange) {
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
    private String extractFromRemoteAddress(ServerWebExchange exchange) {
        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();

        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }

        log.debug("Unable to extract client IP from RemoteAddress. Returning 'unknown'.");
        return UNKNOWN_IP;
    }

    /**
     * IP 주소 형식 검증 (Java 표준 라이브러리 사용)
     *
     * <p>IPv4, IPv6, IPv4-mapped IPv6 (::ffff:1.2.3.4) 등 모든 표준 형식 지원
     *
     * @param ip 검증할 IP 문자열
     * @return 유효한 IP 주소 형식이면 true
     */
    private boolean isValidIpAddress(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }

        try {
            InetAddress.getByName(ip);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
