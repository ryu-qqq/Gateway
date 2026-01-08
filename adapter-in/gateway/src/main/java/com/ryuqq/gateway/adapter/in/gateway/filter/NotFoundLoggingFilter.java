package com.ryuqq.gateway.adapter.in.gateway.filter;

import com.ryuqq.gateway.adapter.in.gateway.common.util.ClientIpExtractor;
import com.ryuqq.gateway.adapter.in.gateway.config.GatewayFilterOrder;
import com.ryuqq.gateway.adapter.in.gateway.metrics.GatewayMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 404 Not Found 요청 로깅 필터
 *
 * <p>존재하지 않는 경로로 요청하는 봇/스캐너를 탐지하기 위한 로깅 필터입니다.
 *
 * <p><strong>로깅 대상</strong>:
 *
 * <ul>
 *   <li>404 Not Found 응답
 *   <li>잠재적인 취약점 스캔 경로 (wp-admin, .env, phpMyAdmin 등)
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class NotFoundLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(NotFoundLoggingFilter.class);

    private final ClientIpExtractor clientIpExtractor;
    private final GatewayMetrics gatewayMetrics;

    public NotFoundLoggingFilter(
            ClientIpExtractor clientIpExtractor, GatewayMetrics gatewayMetrics) {
        this.clientIpExtractor = clientIpExtractor;
        this.gatewayMetrics = gatewayMetrics;
    }

    @Override
    public int getOrder() {
        return GatewayFilterOrder.RESPONSE_LOGGING_FILTER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange)
                .then(
                        Mono.fromRunnable(
                                () -> {
                                    HttpStatusCode statusCode =
                                            exchange.getResponse().getStatusCode();

                                    if (statusCode == null) {
                                        return;
                                    }

                                    if (HttpStatus.NOT_FOUND.equals(statusCode)) {
                                        logNotFound(exchange);
                                    }
                                }));
    }

    private void logNotFound(ServerWebExchange exchange) {
        String clientIp = clientIpExtractor.extractWithTrustedProxy(exchange);
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();
        String userAgent = exchange.getRequest().getHeaders().getFirst("User-Agent");
        String queryString = exchange.getRequest().getURI().getQuery();

        // 의심스러운 경로 패턴 체크
        boolean isSuspicious = isSuspiciousPath(path);

        // Prometheus 메트릭 기록
        gatewayMetrics.recordNotFound(clientIp, method, path, isSuspicious);

        if (isSuspicious) {
            log.warn(
                    "Suspicious 404 request: ip={}, method={}, path={}, query={}, userAgent={}",
                    clientIp,
                    method,
                    path,
                    queryString,
                    userAgent);
        } else {
            log.info(
                    "Not found: ip={}, method={}, path={}, query={}, userAgent={}",
                    clientIp,
                    method,
                    path,
                    queryString,
                    userAgent);
        }
    }

    /**
     * 의심스러운 경로 패턴 체크
     *
     * <p>일반적인 봇/스캐너가 접근하는 취약점 탐색 경로를 탐지합니다.
     */
    private boolean isSuspiciousPath(String path) {
        if (path == null) {
            return false;
        }

        String lowerPath = path.toLowerCase();

        // WordPress 관련
        if (lowerPath.contains("wp-admin")
                || lowerPath.contains("wp-login")
                || lowerPath.contains("wp-includes")
                || lowerPath.contains("wp-content")
                || lowerPath.contains("xmlrpc.php")) {
            return true;
        }

        // PHP 관련
        if (lowerPath.contains("phpmyadmin")
                || lowerPath.contains("phpinfo")
                || lowerPath.contains(".php")) {
            return true;
        }

        // 환경 파일 / 설정 파일
        if (lowerPath.contains(".env")
                || lowerPath.contains(".git")
                || lowerPath.contains(".svn")
                || lowerPath.contains("config.json")
                || lowerPath.contains(".aws")
                || lowerPath.contains(".ssh")) {
            return true;
        }

        // 관리자 페이지
        if (lowerPath.contains("/admin")
                || lowerPath.contains("/manager")
                || lowerPath.contains("/console")) {
            return true;
        }

        // 백업 파일
        if (lowerPath.endsWith(".bak")
                || lowerPath.endsWith(".backup")
                || lowerPath.endsWith(".sql")
                || lowerPath.endsWith(".tar.gz")
                || lowerPath.endsWith(".zip")) {
            return true;
        }

        // 기타 일반적인 취약점 탐색 경로
        return lowerPath.contains("/cgi-bin")
                || lowerPath.contains("/shell")
                || lowerPath.contains("/eval")
                || lowerPath.contains("/.well-known/security.txt")
                || lowerPath.contains("/actuator") && !lowerPath.startsWith("/actuator");
    }
}
