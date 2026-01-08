package com.ryuqq.gateway.adapter.in.gateway.filter;

import com.ryuqq.gateway.adapter.in.gateway.common.util.GatewayErrorResponder;
import com.ryuqq.gateway.adapter.in.gateway.config.GatewayFilterOrder;
import com.ryuqq.gateway.application.authentication.dto.command.RefreshAccessTokenCommand;
import com.ryuqq.gateway.application.authentication.port.in.command.RefreshAccessTokenUseCase;
import com.ryuqq.gateway.application.authentication.port.out.client.AuthHubClient;
import com.ryuqq.gateway.domain.authentication.exception.RefreshTokenExpiredException;
import com.ryuqq.gateway.domain.authentication.exception.RefreshTokenInvalidException;
import com.ryuqq.gateway.domain.authentication.exception.RefreshTokenMissingException;
import com.ryuqq.gateway.domain.authentication.exception.RefreshTokenReusedException;
import com.ryuqq.gateway.domain.authentication.exception.TokenRefreshFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
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
 * <p><strong>실행 순서</strong>: JWT_AUTH_FILTER 이전 (HIGHEST_PRECEDENCE + 2)
 *
 * <p><strong>실행 조건</strong>:
 *
 * <ul>
 *   <li>Authorization 헤더에 Bearer 토큰이 존재
 *   <li>Cookie에 refresh_token이 존재
 *   <li>Access Token이 만료된 경우 (AuthHub에서 확인)
 * </ul>
 *
 * <p><strong>처리 흐름</strong>:
 *
 * <ol>
 *   <li>Authorization 헤더에서 Access Token 추출
 *   <li>Cookie에서 refresh_token 추출
 *   <li>AuthHubClient로 Access Token 만료 여부 확인
 *   <li>만료된 경우 RefreshAccessTokenUseCase 호출
 *   <li>새 Access Token → Authorization 헤더 교체
 *   <li>새 Refresh Token → Cookie 설정 (HttpOnly, Secure)
 *   <li>다음 Filter(JwtAuthenticationFilter)로 전달
 * </ol>
 *
 * <p><strong>에러 처리</strong>:
 *
 * <ul>
 *   <li>RefreshTokenMissingException → 401 (Cookie에 token 없음)
 *   <li>RefreshTokenReusedException → 401 (재사용 감지 - 탈취 의심)
 *   <li>RefreshTokenExpiredException → 401 (Refresh Token 만료)
 *   <li>RefreshTokenInvalidException → 401 (Refresh Token 유효하지 않음)
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
    private final AuthHubClient authHubClient;
    private final GatewayErrorResponder errorResponder;

    public TokenRefreshFilter(
            RefreshAccessTokenUseCase refreshAccessTokenUseCase,
            AuthHubClient authHubClient,
            GatewayErrorResponder errorResponder) {
        this.refreshAccessTokenUseCase = refreshAccessTokenUseCase;
        this.authHubClient = authHubClient;
        this.errorResponder = errorResponder;
    }

    @Override
    public int getOrder() {
        return GatewayFilterOrder.TOKEN_REFRESH_FILTER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Authorization 헤더가 없으면 Refresh 대상 아님 → 다음 필터로
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return chain.filter(exchange);
        }

        // Cookie에서 refresh_token 추출
        HttpCookie refreshTokenCookie =
                exchange.getRequest().getCookies().getFirst(REFRESH_TOKEN_COOKIE);

        // Refresh Token이 없으면 갱신 불가 → 다음 필터로 (JWT_AUTH_FILTER에서 처리)
        if (refreshTokenCookie == null) {
            log.debug("No refresh token cookie found, skipping token refresh");
            return chain.filter(exchange);
        }

        String accessToken = authHeader.substring(BEARER_PREFIX.length());
        String refreshTokenValue = refreshTokenCookie.getValue();

        // AuthHub에서 Access Token 만료 여부 확인
        return authHubClient
                .extractExpiredTokenInfo(accessToken)
                .flatMap(
                        tokenInfo -> {
                            // 토큰이 만료되지 않았으면 갱신 불필요 → 다음 필터로
                            if (!tokenInfo.isExpired()) {
                                log.debug("Access token is not expired, skipping token refresh");
                                return chain.filter(exchange);
                            }

                            // 만료된 토큰에서 userId와 tenantId 추출
                            Long userId = tokenInfo.userId();
                            String tenantId = tokenInfo.tenantId();

                            if (userId == null || tenantId == null) {
                                log.warn("Could not extract userId or tenantId from expired token");
                                return chain.filter(exchange);
                            }

                            log.debug(
                                    "Access token expired, attempting refresh for tenant:{},"
                                            + " user:{}",
                                    tenantId,
                                    userId);

                            // Token Refresh 실행
                            return executeTokenRefresh(
                                    exchange, chain, tenantId, userId, refreshTokenValue);
                        })
                .onErrorResume(
                        e -> {
                            // AuthHub 호출 실패 시 (예: 유효하지 않은 서명) → 다음 필터로 (JWT_AUTH_FILTER에서 처리)
                            log.debug(
                                    "Failed to extract token info, delegating to JWT auth filter:"
                                            + " {}",
                                    e.getMessage());
                            return chain.filter(exchange);
                        });
    }

    /** Token Refresh 실행 */
    private Mono<Void> executeTokenRefresh(
            ServerWebExchange exchange,
            GatewayFilterChain chain,
            String tenantId,
            Long userId,
            String refreshTokenValue) {

        RefreshAccessTokenCommand command =
                new RefreshAccessTokenCommand(tenantId, userId, refreshTokenValue);

        return refreshAccessTokenUseCase
                .execute(command)
                .flatMap(
                        response -> {
                            log.debug(
                                    "Token refresh successful for tenant:{}, user:{}",
                                    tenantId,
                                    userId);

                            // 새 Access Token으로 Authorization 헤더 업데이트
                            ServerHttpRequest mutatedRequest =
                                    exchange.getRequest()
                                            .mutate()
                                            .header(
                                                    HttpHeaders.AUTHORIZATION,
                                                    BEARER_PREFIX + response.accessTokenValue())
                                            .header(X_USER_ID_HEADER, String.valueOf(userId))
                                            .build();

                            // 새 Refresh Token Cookie 설정
                            ResponseCookie newRefreshTokenCookie =
                                    ResponseCookie.from(
                                                    REFRESH_TOKEN_COOKIE,
                                                    response.refreshTokenValue())
                                            .httpOnly(true)
                                            .secure(true)
                                            .path(COOKIE_PATH)
                                            .maxAge(
                                                    java.time.Duration.ofDays(
                                                            REFRESH_TOKEN_MAX_AGE_DAYS))
                                            .sameSite(COOKIE_SAME_SITE)
                                            .build();

                            ServerWebExchange mutatedExchange =
                                    exchange.mutate().request(mutatedRequest).build();

                            mutatedExchange.getResponse().addCookie(newRefreshTokenCookie);

                            // Exchange Attribute 업데이트 (다음 필터에서 사용)
                            mutatedExchange
                                    .getAttributes()
                                    .put(USER_ID_ATTRIBUTE, String.valueOf(userId));
                            mutatedExchange.getAttributes().put(TENANT_ID_ATTRIBUTE, tenantId);

                            return chain.filter(mutatedExchange)
                                    .contextWrite(
                                            ctx ->
                                                    ctx.put(
                                                            USER_ID_ATTRIBUTE,
                                                            String.valueOf(userId)));
                        })
                .onErrorResume(this::isRefreshTokenError, e -> handleRefreshError(exchange, e));
    }

    /** Refresh Token 관련 에러인지 확인 */
    private boolean isRefreshTokenError(Throwable e) {
        return e instanceof RefreshTokenMissingException
                || e instanceof RefreshTokenReusedException
                || e instanceof RefreshTokenExpiredException
                || e instanceof RefreshTokenInvalidException
                || e instanceof TokenRefreshFailedException;
    }

    /** Refresh 에러 처리 */
    private Mono<Void> handleRefreshError(ServerWebExchange exchange, Throwable e) {
        if (e instanceof RefreshTokenReusedException) {
            log.warn("Refresh token reuse detected - possible token theft");
            return unauthorized(
                    exchange,
                    "TOKEN_REUSE_DETECTED",
                    "Refresh token reuse detected. Please login again.");
        }

        if (e instanceof RefreshTokenExpiredException) {
            log.debug("Refresh token expired");
            return unauthorized(
                    exchange,
                    "REFRESH_TOKEN_EXPIRED",
                    "Refresh token has expired. Please login again.");
        }

        if (e instanceof RefreshTokenInvalidException) {
            log.debug("Refresh token invalid");
            return unauthorized(
                    exchange,
                    "REFRESH_TOKEN_INVALID",
                    "Refresh token is invalid. Please login again.");
        }

        if (e instanceof RefreshTokenMissingException) {
            log.debug("Refresh token missing");
            return unauthorized(
                    exchange,
                    "REFRESH_TOKEN_MISSING",
                    "Refresh token is required. Please login again.");
        }

        if (e instanceof TokenRefreshFailedException) {
            log.error("Token refresh failed", e);
            return serverError(
                    exchange, "TOKEN_REFRESH_FAILED", "Failed to refresh token. Please try again.");
        }

        log.error("Unexpected error during token refresh", e);
        return unauthorized(exchange, "UNAUTHORIZED", "Authentication required");
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String code, String message) {
        return errorResponder.unauthorized(exchange, code, message);
    }

    private Mono<Void> serverError(ServerWebExchange exchange, String code, String message) {
        return errorResponder.internalServerError(exchange, code, message);
    }
}
