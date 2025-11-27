package com.ryuqq.gateway.adapter.in.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ryuqq.gateway.adapter.in.gateway.common.dto.ApiResponse;
import com.ryuqq.gateway.adapter.in.gateway.common.dto.ErrorInfo;
import com.ryuqq.gateway.adapter.in.gateway.config.GatewayFilterOrder;
import com.ryuqq.gateway.application.authentication.dto.command.RefreshAccessTokenCommand;
import com.ryuqq.gateway.application.authentication.port.in.command.RefreshAccessTokenUseCase;
import com.ryuqq.gateway.domain.authentication.exception.RefreshTokenMissingException;
import com.ryuqq.gateway.domain.authentication.exception.RefreshTokenReusedException;
import com.ryuqq.gateway.domain.authentication.exception.TokenRefreshFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Token Refresh Filter
 *
 * <p>JWT Access Token 만료 시 자동으로 Refresh Token을 사용하여 새 Token을 발급합니다.
 *
 * <p><strong>실행 조건</strong>:
 *
 * <ul>
 *   <li>JwtAuthenticationFilter 이후 실행 (HIGHEST_PRECEDENCE + 4)
 *   <li>userId가 Exchange Attribute에 없는 경우 (JWT 검증 실패 = 만료)
 *   <li>Cookie에 refresh_token이 존재하는 경우
 * </ul>
 *
 * <p><strong>처리 흐름</strong>:
 *
 * <ol>
 *   <li>Cookie에서 refresh_token 추출
 *   <li>RefreshAccessTokenUseCase 호출
 *   <li>새 Access Token → Authorization 헤더 설정
 *   <li>새 Refresh Token → Cookie 설정 (HttpOnly, Secure)
 *   <li>Exchange Attribute 업데이트
 *   <li>다음 Filter로 전달
 * </ol>
 *
 * <p><strong>에러 처리</strong>:
 *
 * <ul>
 *   <li>RefreshTokenMissingException → 401 (Cookie에 token 없음)
 *   <li>RefreshTokenReusedException → 401 (재사용 감지 - 탈취 의심)
 *   <li>TokenRefreshFailedException → 500 (서버 오류)
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class TokenRefreshFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(TokenRefreshFilter.class);

    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    private static final String USER_ID_ATTRIBUTE = "userId";
    private static final String TENANT_ID_ATTRIBUTE = "tenantId";
    private static final String X_USER_ID_HEADER = "X-User-Id";
    private static final String BEARER_PREFIX = "Bearer ";

    /** Refresh Token Cookie 설정 */
    private static final int REFRESH_TOKEN_MAX_AGE_DAYS = 7;
    private static final String COOKIE_PATH = "/";
    private static final String COOKIE_SAME_SITE = "Strict";

    private final RefreshAccessTokenUseCase refreshAccessTokenUseCase;
    private final ObjectMapper objectMapper;

    public TokenRefreshFilter(
            RefreshAccessTokenUseCase refreshAccessTokenUseCase,
            ObjectMapper objectMapper) {
        this.refreshAccessTokenUseCase = refreshAccessTokenUseCase;
        this.objectMapper = objectMapper;
    }

    @Override
    public int getOrder() {
        return GatewayFilterOrder.TOKEN_REFRESH_FILTER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // userId가 이미 있으면 JWT 검증 성공 → Refresh 불필요
        if (exchange.getAttribute(USER_ID_ATTRIBUTE) != null) {
            return chain.filter(exchange);
        }

        // Authorization 헤더가 없으면 Refresh 대상 아님
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return chain.filter(exchange);
        }

        // Cookie에서 refresh_token 추출
        HttpCookie refreshTokenCookie = exchange.getRequest()
                .getCookies()
                .getFirst(REFRESH_TOKEN_COOKIE);

        if (refreshTokenCookie == null) {
            log.debug("Refresh token cookie not found");
            return chain.filter(exchange);
        }

        String refreshTokenValue = refreshTokenCookie.getValue();
        String tenantId = exchange.getAttribute(TENANT_ID_ATTRIBUTE);

        // tenantId가 없으면 TenantIsolationFilter에서 설정되기 전 → 에러
        if (tenantId == null) {
            log.warn("TenantId not found in exchange attributes for token refresh");
            return unauthorized(exchange, "TENANT_REQUIRED", "Tenant identification required");
        }

        // userId를 Header에서 추출 시도 (기존 만료된 JWT에서)
        Long userId = extractUserIdFromExpiredToken(exchange);
        if (userId == null) {
            log.warn("UserId could not be extracted for token refresh");
            return unauthorized(exchange, "USER_ID_REQUIRED", "User identification required");
        }

        return executeTokenRefresh(exchange, chain, tenantId, userId, refreshTokenValue);
    }

    /**
     * Token Refresh 실행
     */
    private Mono<Void> executeTokenRefresh(
            ServerWebExchange exchange,
            GatewayFilterChain chain,
            String tenantId,
            Long userId,
            String refreshTokenValue) {

        RefreshAccessTokenCommand command = new RefreshAccessTokenCommand(
                tenantId, userId, refreshTokenValue);

        return refreshAccessTokenUseCase.execute(command)
                .flatMap(response -> {
                    log.debug("Token refresh successful for tenant:{}, user:{}", tenantId, userId);

                    // 새 Access Token으로 Authorization 헤더 업데이트
                    ServerHttpRequest mutatedRequest = exchange.getRequest()
                            .mutate()
                            .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + response.accessTokenValue())
                            .header(X_USER_ID_HEADER, String.valueOf(userId))
                            .build();

                    // 새 Refresh Token Cookie 설정
                    ResponseCookie newRefreshTokenCookie = ResponseCookie
                            .from(REFRESH_TOKEN_COOKIE, response.refreshTokenValue())
                            .httpOnly(true)
                            .secure(true)
                            .path(COOKIE_PATH)
                            .maxAge(java.time.Duration.ofDays(REFRESH_TOKEN_MAX_AGE_DAYS))
                            .sameSite(COOKIE_SAME_SITE)
                            .build();

                    ServerWebExchange mutatedExchange = exchange.mutate()
                            .request(mutatedRequest)
                            .build();

                    mutatedExchange.getResponse().addCookie(newRefreshTokenCookie);

                    // Exchange Attribute 업데이트
                    mutatedExchange.getAttributes().put(USER_ID_ATTRIBUTE, String.valueOf(userId));

                    return chain.filter(mutatedExchange)
                            .contextWrite(ctx -> ctx.put(USER_ID_ATTRIBUTE, String.valueOf(userId)));
                })
                .onErrorResume(this::isRefreshTokenError, e -> handleRefreshError(exchange, e));
    }

    /**
     * 만료된 JWT에서 userId 추출 시도
     *
     * <p>JWT가 만료되었더라도 payload는 복호화 가능 (서명 검증만 실패)
     */
    private Long extractUserIdFromExpiredToken(ServerWebExchange exchange) {
        // 이 시점에서 JwtAuthenticationFilter가 userId를 추출하지 못했으므로
        // 별도 방법으로 추출 필요 - 여기서는 X-User-Id 헤더 또는 쿼리 파라미터 등에서 추출
        // 실제 구현에서는 만료된 JWT의 payload를 직접 파싱하거나 별도 claim 사용

        // 간단한 구현: exchange attribute에서 시도 (이전 Filter에서 설정했을 수 있음)
        Object userIdObj = exchange.getAttribute(USER_ID_ATTRIBUTE);
        if (userIdObj instanceof Long) {
            return (Long) userIdObj;
        }
        if (userIdObj instanceof String userIdStr) {
            try {
                return Long.parseLong(userIdStr);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        // 헤더에서 추출 시도
        String userIdHeader = exchange.getRequest().getHeaders().getFirst(X_USER_ID_HEADER);
        if (userIdHeader != null) {
            try {
                return Long.parseLong(userIdHeader);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return null;
    }

    /**
     * Refresh Token 관련 에러인지 확인
     */
    private boolean isRefreshTokenError(Throwable e) {
        return e instanceof RefreshTokenMissingException
                || e instanceof RefreshTokenReusedException
                || e instanceof TokenRefreshFailedException;
    }

    /**
     * Refresh 에러 처리
     */
    private Mono<Void> handleRefreshError(ServerWebExchange exchange, Throwable e) {
        if (e instanceof RefreshTokenReusedException) {
            log.warn("Refresh token reuse detected - possible token theft");
            return unauthorized(exchange, "TOKEN_REUSE_DETECTED",
                    "Refresh token reuse detected. Please login again.");
        }

        if (e instanceof TokenRefreshFailedException) {
            log.error("Token refresh failed", e);
            return serverError(exchange, "TOKEN_REFRESH_FAILED",
                    "Failed to refresh token. Please try again.");
        }

        log.error("Unexpected error during token refresh", e);
        return unauthorized(exchange, "UNAUTHORIZED", "Authentication required");
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String code, String message) {
        return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, code, message);
    }

    private Mono<Void> serverError(ServerWebExchange exchange, String code, String message) {
        return writeErrorResponse(exchange, HttpStatus.INTERNAL_SERVER_ERROR, code, message);
    }

    private Mono<Void> writeErrorResponse(
            ServerWebExchange exchange,
            HttpStatus status,
            String code,
            String message) {

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ErrorInfo error = new ErrorInfo(code, message);
        ApiResponse<Void> errorResponse = ApiResponse.ofFailure(error);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            return exchange.getResponse().setComplete();
        }
    }
}
