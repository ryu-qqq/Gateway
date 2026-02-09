package com.ryuqq.gateway.adapter.in.gateway.filter;

import com.ryuqq.gateway.adapter.in.gateway.common.util.GatewayErrorResponder;
import com.ryuqq.gateway.adapter.in.gateway.config.GatewayFilterOrder;
import com.ryuqq.gateway.application.authorization.dto.command.ValidatePermissionCommand;
import com.ryuqq.gateway.application.authorization.port.in.command.ValidatePermissionUseCase;
import com.ryuqq.gateway.domain.authorization.exception.PermissionDeniedException;
import com.ryuqq.gateway.domain.authorization.exception.PermissionSpecNotFoundException;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Permission Filter
 *
 * <p>Spring Cloud Gateway GlobalFilter로 권한 검사를 수행합니다.
 *
 * <p><strong>실행 순서</strong>: JWT 인증 필터 이후 (Order: HIGHEST_PRECEDENCE + 5)
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>JWT 인증된 사용자 정보로 권한 검증
 *   <li>Permission Spec 기반 엔드포인트 권한 확인
 *   <li>Permission Hash 기반 사용자 권한 검증
 *   <li>Public 엔드포인트 바이패스
 *   <li>권한 부족 시 403 Forbidden 응답
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class PermissionFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(PermissionFilter.class);

    private static final String USER_ID_ATTRIBUTE = "userId";
    private static final String TENANT_ID_ATTRIBUTE = "tenantId";
    private static final String PERMISSION_HASH_ATTRIBUTE = "permissionHash";
    private static final String ROLES_ATTRIBUTE = "roles";
    private static final String SUPER_ADMIN_ROLE = "SUPER_ADMIN";

    private final ValidatePermissionUseCase validatePermissionUseCase;
    private final GatewayErrorResponder errorResponder;

    public PermissionFilter(
            ValidatePermissionUseCase validatePermissionUseCase,
            GatewayErrorResponder errorResponder) {
        this.validatePermissionUseCase = validatePermissionUseCase;
        this.errorResponder = errorResponder;
    }

    @Override
    public int getOrder() {
        return GatewayFilterOrder.PERMISSION_FILTER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String userId = exchange.getAttribute(USER_ID_ATTRIBUTE);

        // userId가 없으면 (Public API) 권한 검사 스킵
        if (userId == null) {
            return chain.filter(exchange);
        }

        String tenantId = exchange.getAttribute(TENANT_ID_ATTRIBUTE);
        String permissionHash = exchange.getAttribute(PERMISSION_HASH_ATTRIBUTE);
        Set<String> roles = exchange.getAttribute(ROLES_ATTRIBUTE);

        // SUPER_ADMIN은 모든 권한 검사 bypass
        if (roles != null && roles.contains(SUPER_ADMIN_ROLE)) {
            log.debug("SUPER_ADMIN bypass: userId={}, path={}", userId, exchange.getRequest().getURI().getPath());
            return chain.filter(exchange);
        }

        String requestPath = exchange.getRequest().getURI().getPath();
        String requestMethod = exchange.getRequest().getMethod().name();

        ValidatePermissionCommand command =
                ValidatePermissionCommand.of(
                        userId, tenantId, permissionHash, roles, requestPath, requestMethod);

        return validatePermissionUseCase
                .execute(command)
                .flatMap(
                        response -> {
                            if (response.authorized()) {
                                log.debug(
                                        "Permission granted: userId={}, path={}, method={}",
                                        userId,
                                        requestPath,
                                        requestMethod);
                                return chain.filter(exchange);
                            }
                            return forbidden(exchange, "권한이 없습니다");
                        })
                .onErrorResume(
                        PermissionDeniedException.class,
                        e -> {
                            log.warn(
                                    "Permission denied: userId={}, path={}, method={}, required={},"
                                            + " has={}",
                                    userId,
                                    requestPath,
                                    requestMethod,
                                    e.requiredPermissions(),
                                    e.userPermissions());
                            return forbidden(exchange, "요청한 리소스에 대한 권한이 없습니다");
                        })
                .onErrorResume(
                        PermissionSpecNotFoundException.class,
                        e -> {
                            log.warn(
                                    "Permission spec not found: path={}, method={}",
                                    requestPath,
                                    requestMethod);
                            return forbidden(exchange, "요청한 엔드포인트를 찾을 수 없습니다");
                        })
                .onErrorResume(
                        e -> {
                            log.error(
                                    "Permission validation error: path={}, method={}, error={}",
                                    requestPath,
                                    requestMethod,
                                    e.getMessage());
                            return internalError(exchange);
                        });
    }

    private Mono<Void> forbidden(ServerWebExchange exchange, String message) {
        return errorResponder.forbidden(exchange, "FORBIDDEN", message);
    }

    private Mono<Void> internalError(ServerWebExchange exchange) {
        return errorResponder.internalServerError(exchange, "INTERNAL_ERROR", "권한 검증 중 오류가 발생했습니다");
    }
}
