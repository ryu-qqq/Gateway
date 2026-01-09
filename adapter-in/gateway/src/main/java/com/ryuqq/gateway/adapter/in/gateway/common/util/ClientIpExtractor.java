package com.ryuqq.gateway.adapter.in.gateway.common.util;

import java.net.InetSocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

/**
 * 클라이언트 IP 추출 유틸리티
 *
 * <p>AWS CloudFront/ALB 환경에서 클라이언트 IP를 안전하게 추출합니다.
 *
 * <p><strong>헤더 우선순위</strong>:
 *
 * <ol>
 *   <li>CloudFront-Viewer-Address: CloudFront가 설정하는 신뢰할 수 있는 헤더 (IP:port 형식)
 *   <li>X-Forwarded-For: 표준 프록시 헤더 (첫 번째 IP 사용)
 *   <li>RemoteAddress: 직접 연결된 클라이언트 IP (fallback)
 * </ol>
 *
 * <p><strong>보안 고려사항</strong>:
 *
 * <ul>
 *   <li>CloudFront-Viewer-Address는 CloudFront가 설정하므로 신뢰 가능
 *   <li>X-Forwarded-For는 클라이언트가 조작 가능 → CloudFront 뒤에서만 신뢰
 *   <li>프록시 없는 환경에서는 RemoteAddress만 사용
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class ClientIpExtractor {

    private static final Logger log = LoggerFactory.getLogger(ClientIpExtractor.class);

    private static final String CLOUDFRONT_VIEWER_ADDRESS = "CloudFront-Viewer-Address";
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
     * <p>AWS CloudFront/ALB 뒤에서 사용합니다.
     *
     * <p><strong>추출 우선순위</strong>:
     *
     * <ol>
     *   <li>CloudFront-Viewer-Address (IP:port 형식, CloudFront가 설정)
     *   <li>X-Forwarded-For (첫 번째 IP)
     *   <li>RemoteAddress (fallback)
     * </ol>
     *
     * @param exchange ServerWebExchange
     * @return 클라이언트 IP (추출 불가 시 "unknown")
     */
    public String extractWithTrustedProxy(ServerWebExchange exchange) {
        // 1. CloudFront-Viewer-Address 우선 (가장 신뢰할 수 있는 헤더)
        String viewerAddress =
                exchange.getRequest().getHeaders().getFirst(CLOUDFRONT_VIEWER_ADDRESS);
        if (viewerAddress != null && !viewerAddress.isBlank()) {
            String ip = extractIpFromViewerAddress(viewerAddress);
            if (ip != null && isValidIpAddress(ip)) {
                return ip;
            }
            log.debug(
                    "Invalid CloudFront-Viewer-Address format: '{}'. Trying X-Forwarded-For.",
                    viewerAddress);
        }

        // 2. X-Forwarded-For 헤더 확인
        String xForwardedFor = exchange.getRequest().getHeaders().getFirst(X_FORWARDED_FOR_HEADER);
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            String firstIp = xForwardedFor.split(",")[0].trim();
            if (isValidIpAddress(firstIp)) {
                return firstIp;
            }
            log.debug(
                    "Invalid IP format in X-Forwarded-For header: '{}'. Falling back to"
                            + " RemoteAddress.",
                    firstIp);
        } else {
            log.debug(
                    "X-Forwarded-For header is missing or empty. Available headers: {}",
                    exchange.getRequest().getHeaders().keySet());
        }

        // 3. RemoteAddress fallback
        return extractFromRemoteAddress(exchange);
    }

    /**
     * CloudFront-Viewer-Address에서 IP 추출
     *
     * <p>형식: "IP:port" (예: "192.0.2.1:46532" 또는 "[2001:db8::1]:8080")
     *
     * @param viewerAddress CloudFront-Viewer-Address 헤더 값
     * @return IP 주소 또는 null
     */
    private String extractIpFromViewerAddress(String viewerAddress) {
        if (viewerAddress == null || viewerAddress.isBlank()) {
            return null;
        }

        // IPv6 형식: [2001:db8::1]:8080
        if (viewerAddress.startsWith("[")) {
            int closeBracket = viewerAddress.indexOf(']');
            if (closeBracket > 0) {
                return viewerAddress.substring(1, closeBracket);
            }
        }

        // IPv4 형식: 192.0.2.1:46532
        int lastColon = viewerAddress.lastIndexOf(':');
        if (lastColon > 0) {
            return viewerAddress.substring(0, lastColon);
        }

        return viewerAddress;
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

    // IPv4 정규식: 0-255.0-255.0-255.0-255
    private static final String IPV4_PATTERN =
            "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";

    // IPv6 정규식: 간략화된 버전 (::, :: 축약 포함)
    private static final String IPV6_PATTERN =
            "^(([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|"
                + "([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|"
                + "([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|"
                + "([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|"
                + "([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|"
                + "([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|"
                + "[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:)|"
                + "fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]+|"
                + "::(ffff(:0{1,4})?:)?((25[0-5]|(2[0-4]|1?[0-9])?[0-9])\\.){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9])|"
                + "([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1?[0-9])?[0-9])\\.){3}(25[0-5]|(2[0-4]|1?[0-9])?[0-9]))$";

    private static final java.util.regex.Pattern IPV4_COMPILED =
            java.util.regex.Pattern.compile(IPV4_PATTERN);
    private static final java.util.regex.Pattern IPV6_COMPILED =
            java.util.regex.Pattern.compile(IPV6_PATTERN);

    /**
     * IP 주소 형식 검증 (정규식 기반 - DNS 조회 없음)
     *
     * <p>IPv4, IPv6, IPv4-mapped IPv6 (::ffff:1.2.3.4) 등 모든 표준 형식 지원 InetAddress.getByName() 대신
     * 정규식을 사용하여 악의적인 입력에 대한 DNS 조회를 방지합니다.
     *
     * @param ip 검증할 IP 문자열
     * @return 유효한 IP 주소 형식이면 true
     */
    private boolean isValidIpAddress(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }

        return IPV4_COMPILED.matcher(ip).matches() || IPV6_COMPILED.matcher(ip).matches();
    }
}
