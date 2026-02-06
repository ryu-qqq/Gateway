package com.ryuqq.gateway.adapter.in.gateway.filter;

import com.ryuqq.gateway.adapter.in.gateway.common.util.ClientIpExtractor;
import com.ryuqq.gateway.adapter.in.gateway.common.util.GatewayErrorResponder;
import com.ryuqq.gateway.adapter.in.gateway.config.GatewayFilterOrder;
import com.ryuqq.gateway.adapter.in.gateway.config.PublicPathsProperties;
import com.ryuqq.gateway.application.authentication.dto.command.ValidateJwtCommand;
import com.ryuqq.gateway.application.authentication.port.in.command.ValidateJwtUseCase;
import com.ryuqq.gateway.application.ratelimit.dto.command.RecordFailureCommand;
import com.ryuqq.gateway.application.ratelimit.port.in.command.RecordFailureUseCase;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * JWT Authentication Filter
 *
 * <p>Spring Cloud Gateway GlobalFilter로 JWT 인증을 수행합니다.
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>Authorization 헤더에서 Bearer Token 추출
 *   <li>ValidateJwtUseCase를 통한 JWT 검증
 *   <li>ServerWebExchange Attribute 설정 (userId, roles)
 *   <li>Downstream 서비스로 X-User-Id 헤더 전달
 *   <li>Reactor Context에 userId 저장 (로깅용)
 *   <li>JWT 검증 실패 시 RecordFailureUseCase 호출 (IP 차단용)
 * </ul>
 *
 * <p><strong>주의</strong>: Reactive 환경에서는 ThreadLocal 기반 MDC 대신 Reactor Context를 사용해야 합니다.
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_ID_ATTRIBUTE = "userId";
    private static final String TENANT_ID_ATTRIBUTE = "tenantId";
    private static final String ORGANIZATION_ID_ATTRIBUTE = "organizationId";
    private static final String PERMISSION_HASH_ATTRIBUTE = "permissionHash";
    private static final String ROLES_ATTRIBUTE = "roles";
    private static final String PERMISSIONS_ATTRIBUTE = "permissions";
    private static final String MFA_VERIFIED_ATTRIBUTE = "mfaVerified";
    private static final String X_USER_ID_HEADER = "X-User-Id";
    private static final String X_TENANT_ID_HEADER = "X-Tenant-Id";
    private static final String X_ORGANIZATION_ID_HEADER = "X-Organization-Id";
    private static final String X_USER_ROLES_HEADER = "X-User-Roles";
    private static final String X_USER_PERMISSIONS_HEADER = "X-User-Permissions";
    private static final String ACCESS_TOKEN_COOKIE = "access_token";

    private final ValidateJwtUseCase validateJwtUseCase;
    private final RecordFailureUseCase recordFailureUseCase;
    private final AntPathMatcher pathMatcher;
    private final PublicPathsProperties publicPathsProperties;
    private final ClientIpExtractor clientIpExtractor;
    private final GatewayErrorResponder errorResponder;

    /** JWT 인증을 건너뛸 전역 Public 경로 패턴 (Host 기반 서비스 제외) */
    private final List<String> globalPublicPaths;

    public JwtAuthenticationFilter(
            ValidateJwtUseCase validateJwtUseCase,
            RecordFailureUseCase recordFailureUseCase,
            PublicPathsProperties publicPathsProperties,
            ClientIpExtractor clientIpExtractor,
            GatewayErrorResponder errorResponder) {
        this.validateJwtUseCase = validateJwtUseCase;
        this.recordFailureUseCase = recordFailureUseCase;
        this.pathMatcher = new AntPathMatcher();
        this.publicPathsProperties = publicPathsProperties;
        this.globalPublicPaths = publicPathsProperties.getAllPublicPaths();
        this.clientIpExtractor = clientIpExtractor;
        this.errorResponder = errorResponder;
    }

    @Override
    public int getOrder() {
        return GatewayFilterOrder.JWT_AUTH_FILTER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String host = extractHost(exchange);

        // Public Path인 경우 JWT 검증 없이 통과
        if (isPublicPath(path, host)) {
            return chain.filter(exchange);
        }

        String token = extractToken(exchange);

        if (token == null) {
            return unauthorized(exchange);
        }

        return validateJwtUseCase
                .execute(new ValidateJwtCommand(token))
                .flatMap(
                        response -> {
                            // 실제 JWT 토큰이 있지만 검증 실패한 경우에만 Invalid JWT로 기록
                            if (!response.isValid()) {
                                return recordFailureAndUnauthorized(exchange);
                            }

                            var claims = response.jwtClaims();
                            String userId = claims.subject();

                            // Exchange Attribute 설정 (Gateway 내부 사용)
                            exchange.getAttributes().put(USER_ID_ATTRIBUTE, userId);
                            exchange.getAttributes().put(TENANT_ID_ATTRIBUTE, claims.tenantId());
                            exchange.getAttributes()
                                    .put(ORGANIZATION_ID_ATTRIBUTE, claims.organizationId());
                            exchange.getAttributes()
                                    .put(PERMISSION_HASH_ATTRIBUTE, claims.permissionHash());
                            Set<String> rolesSet = new HashSet<>(claims.roles());
                            exchange.getAttributes().put(ROLES_ATTRIBUTE, rolesSet);
                            Set<String> permissionsSet = new HashSet<>(claims.permissions());
                            exchange.getAttributes().put(PERMISSIONS_ATTRIBUTE, permissionsSet);
                            exchange.getAttributes()
                                    .put(MFA_VERIFIED_ATTRIBUTE, claims.mfaVerified());

                            // Downstream 서비스로 사용자 정보 전달 (Header)
                            ServerHttpRequest.Builder requestBuilder =
                                    exchange.getRequest().mutate().header(X_USER_ID_HEADER, userId);

                            // tenantId가 있는 경우 헤더 추가
                            if (claims.tenantId() != null) {
                                requestBuilder.header(X_TENANT_ID_HEADER, claims.tenantId());
                            }

                            // organizationId가 있는 경우 헤더 추가
                            if (claims.organizationId() != null) {
                                requestBuilder.header(
                                        X_ORGANIZATION_ID_HEADER, claims.organizationId());
                            }

                            // roles가 있는 경우 콤마로 구분하여 헤더 추가
                            if (!claims.roles().isEmpty()) {
                                requestBuilder.header(
                                        X_USER_ROLES_HEADER, String.join(",", claims.roles()));
                            }

                            // permissions가 있는 경우 콤마로 구분하여 헤더 추가
                            if (!claims.permissions().isEmpty()) {
                                requestBuilder.header(
                                        X_USER_PERMISSIONS_HEADER,
                                        String.join(",", claims.permissions()));
                            }

                            ServerHttpRequest mutatedRequest = requestBuilder.build();

                            ServerWebExchange mutatedExchange =
                                    exchange.mutate().request(mutatedRequest).build();

                            // Reactor Context에 userId 저장 (로깅 컨텍스트 전파)
                            return chain.filter(mutatedExchange)
                                    .contextWrite(ctx -> ctx.put(USER_ID_ATTRIBUTE, userId));
                        })
                .onErrorResume(e -> recordFailureAndUnauthorized(exchange));
    }

    /**
     * Invalid JWT 실패 기록 후 401 응답 반환
     *
     * <p>RecordFailureUseCase를 호출하여 IP별 실패 횟수를 증가시킵니다. 임계값 초과 시 IP가 차단됩니다.
     */
    private Mono<Void> recordFailureAndUnauthorized(ServerWebExchange exchange) {
        String clientIp = clientIpExtractor.extractWithTrustedProxy(exchange);
        RecordFailureCommand command = RecordFailureCommand.forInvalidJwt(clientIp);

        return recordFailureUseCase.execute(command).then(unauthorized(exchange));
    }

    /**
     * Access Token 추출 (Authorization 헤더 우선, Cookie 대체)
     *
     * <p>다음 순서로 토큰을 추출합니다:
     *
     * <ol>
     *   <li>Authorization 헤더의 Bearer 토큰
     *   <li>access_token 쿠키
     * </ol>
     *
     * @param exchange ServerWebExchange
     * @return JWT 토큰 문자열 또는 null
     */
    private String extractToken(ServerWebExchange exchange) {
        // 1. Authorization 헤더 우선
        String authHeader =
                exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }

        // 2. Cookie에서 access_token 추출
        HttpCookie cookie =
                exchange.getRequest().getCookies().getFirst(ACCESS_TOKEN_COOKIE);
        if (cookie != null && !cookie.getValue().isEmpty()) {
            return cookie.getValue();
        }

        return null;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        return errorResponder.unauthorized(exchange, "UNAUTHORIZED", "인증이 필요합니다");
    }

    /**
     * Public Path 여부 확인 (Host 인식)
     *
     * <p>다음 순서로 Public Path 여부를 확인합니다:
     *
     * <ol>
     *   <li>전역 Public Paths 매칭 (Host 기반 서비스 제외)
     *   <li>Host 기반 서비스의 Public Paths 매칭
     * </ol>
     *
     * @param path 요청 경로
     * @param host 요청 Host 헤더 (X-Forwarded-Host 우선)
     * @return Public Path이면 true
     */
    private boolean isPublicPath(String path, String host) {
        // 1. 전역 Public Paths 체크 (Host 기반 서비스 제외됨)
        boolean isGlobalPublic =
                globalPublicPaths.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
        if (isGlobalPublic) {
            return true;
        }

        // 2. Host 기반 서비스의 Public Paths 체크
        List<String> hostPublicPaths = publicPathsProperties.getPublicPathsForHost(host);
        return hostPublicPaths.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * 요청에서 Host 추출 (X-Forwarded-Host 우선)
     *
     * <p>CloudFront나 ALB를 통해 들어오는 요청은 X-Forwarded-Host 헤더에 원본 Host가 있습니다. X-Forwarded-Host가 쉼표로 구분된
     * 여러 값을 포함할 수 있으므로 첫 번째 유효한 값만 사용합니다.
     *
     * @param exchange ServerWebExchange
     * @return Host 값 (포트 제외)
     */
    private String extractHost(ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getRequest().getHeaders();

        // X-Forwarded-Host 우선 (CloudFront/ALB)
        String forwardedHost = headers.getFirst("X-Forwarded-Host");
        if (forwardedHost != null && !forwardedHost.isEmpty()) {
            // 쉼표로 구분된 경우 첫 번째 유효한 호스트 사용
            String firstValidHost = extractFirstValidHost(forwardedHost);
            if (firstValidHost != null) {
                return removePort(firstValidHost);
            }
        }

        // 기본 Host 헤더
        String host = headers.getFirst(HttpHeaders.HOST);
        return removePort(host);
    }

    /**
     * 쉼표로 구분된 호스트 목록에서 첫 번째 유효한 호스트 추출
     *
     * <p>빈 값이나 공백만 있는 값은 건너뛰고 첫 번째 유효한 호스트를 반환합니다.
     *
     * @param hosts 쉼표로 구분된 호스트 문자열
     * @return 첫 번째 유효한 호스트 또는 null
     */
    private String extractFirstValidHost(String hosts) {
        for (String host : hosts.split(",")) {
            String trimmed = host.trim();
            if (!trimmed.isEmpty()) {
                return trimmed;
            }
        }
        return null;
    }

    /**
     * Host에서 포트 번호 제거
     *
     * @param host Host 값 (예: "api.set-of.com:443")
     * @return 포트 제거된 Host (예: "api.set-of.com")
     */
    private String removePort(String host) {
        if (host == null) {
            return null;
        }
        int colonIndex = host.indexOf(':');
        return colonIndex > 0 ? host.substring(0, colonIndex) : host;
    }
}
