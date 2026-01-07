package com.ryuqq.gateway.adapter.in.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ryuqq.gateway.adapter.in.gateway.common.dto.ApiResponse;
import com.ryuqq.gateway.adapter.in.gateway.common.dto.ErrorInfo;
import com.ryuqq.gateway.adapter.in.gateway.config.GatewayFilterOrder;
import com.ryuqq.gateway.application.ratelimit.dto.command.CheckRateLimitCommand;
import com.ryuqq.gateway.application.ratelimit.port.in.command.CheckRateLimitUseCase;
import com.ryuqq.gateway.domain.ratelimit.exception.RateLimitExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * User Rate Limit Filter
 *
 * <p>JWT 인증 후 사용자별 Rate Limiting을 수행하는 GlobalFilter입니다.
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>JwtAuthenticationFilter에서 설정한 userId 조회
 *   <li>User 기반 Rate Limit 체크
 *   <li>Rate Limit 초과 시 429 응답
 * </ul>
 *
 * <p><strong>실행 순서</strong>: JwtAuthenticationFilter 이후에 실행
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class UserRateLimitFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(UserRateLimitFilter.class);

    private static final String USER_ID_ATTRIBUTE = "userId";
    private static final String X_RATE_LIMIT_LIMIT_HEADER = "X-RateLimit-Limit";
    private static final String X_RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";
    private static final String RETRY_AFTER_HEADER = "Retry-After";

    private final CheckRateLimitUseCase checkRateLimitUseCase;
    private final ObjectMapper objectMapper;

    public UserRateLimitFilter(
            CheckRateLimitUseCase checkRateLimitUseCase, ObjectMapper objectMapper) {
        this.checkRateLimitUseCase = checkRateLimitUseCase;
        this.objectMapper = objectMapper;
    }

    @Override
    public int getOrder() {
        return GatewayFilterOrder.USER_RATE_LIMIT_FILTER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String userId = exchange.getAttribute(USER_ID_ATTRIBUTE);

        // userId가 없으면 (JWT 인증 실패 또는 Public API) 통과
        if (userId == null || userId.isBlank()) {
            return chain.filter(exchange);
        }

        // User Rate Limit 체크
        CheckRateLimitCommand userCommand = CheckRateLimitCommand.forUser(userId);

        return checkRateLimitUseCase
                .execute(userCommand)
                .flatMap(
                        response -> {
                            if (!response.allowed()) {
                                return tooManyRequests(
                                        exchange, response.limit(), response.retryAfterSeconds());
                            }

                            // User Rate Limit 헤더 추가 (기존 헤더 덮어쓰기)
                            addRateLimitHeaders(exchange, response.limit(), response.remaining());

                            return chain.filter(exchange);
                        })
                .onErrorResume(
                        RateLimitExceededException.class,
                        e -> tooManyRequests(exchange, e.limit(), e.retryAfterSeconds()))
                .onErrorResume(
                        e -> {
                            // Redis 장애 등 예외 상황에서는 요청을 통과시킴 (fail-open)
                            log.warn(
                                    "User rate limit check failed for userId='{}'. Allowing"
                                            + " request. Error: {}",
                                    userId,
                                    e.getMessage());
                            return chain.filter(exchange);
                        });
    }

    /**
     * Rate Limit 헤더 추가
     *
     * <p>Actuator 경로는 응답이 이미 커밋된 상태일 수 있으므로 스킵합니다. 일반 경로에서도 응답이 이미 커밋된 경우
     * UnsupportedOperationException이 발생할 수 있으므로 예외 처리로 방어합니다.
     */
    private void addRateLimitHeaders(ServerWebExchange exchange, int limit, int remaining) {
        String path = exchange.getRequest().getURI().getPath();

        // Actuator 경로는 스킵 (ReadOnlyHttpHeaders 예외 방지)
        if (isActuatorPath(path)) {
            return;
        }

        // 응답이 이미 커밋된 경우 헤더 설정 불가 - 예외 방어
        try {
            if (!exchange.getResponse().isCommitted()) {
                exchange.getResponse()
                        .getHeaders()
                        .set(X_RATE_LIMIT_LIMIT_HEADER, String.valueOf(limit));
                exchange.getResponse()
                        .getHeaders()
                        .set(X_RATE_LIMIT_REMAINING_HEADER, String.valueOf(remaining));
            }
        } catch (UnsupportedOperationException e) {
            // ReadOnlyHttpHeaders 예외 무시 (응답 이미 커밋됨)
            log.debug("Unable to set rate limit headers - response already committed: {}", path);
        }
    }

    /**
     * Actuator 경로인지 확인
     *
     * @param path 요청 경로
     * @return actuator 경로이면 true
     */
    private boolean isActuatorPath(String path) {
        return path != null && path.startsWith("/actuator");
    }

    /** 429 Too Many Requests 응답 */
    private Mono<Void> tooManyRequests(
            ServerWebExchange exchange, int limit, int retryAfterSeconds) {
        return Mono.defer(
                () -> {
                    // 응답이 이미 커밋된 경우 스킵
                    if (exchange.getResponse().isCommitted()) {
                        return Mono.empty();
                    }

                    exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                    exchange.getResponse()
                            .getHeaders()
                            .add(X_RATE_LIMIT_LIMIT_HEADER, String.valueOf(limit));
                    exchange.getResponse().getHeaders().add(X_RATE_LIMIT_REMAINING_HEADER, "0");
                    exchange.getResponse()
                            .getHeaders()
                            .add(RETRY_AFTER_HEADER, String.valueOf(retryAfterSeconds));

                    ErrorInfo error =
                            new ErrorInfo(
                                    "USER_RATE_LIMIT_EXCEEDED",
                                    "사용자 요청 빈도가 너무 높습니다. 잠시 후 다시 시도해주세요.");
                    ApiResponse<Void> errorResponse = ApiResponse.ofFailure(error);

                    return writeResponse(exchange, errorResponse);
                });
    }

    /**
     * JSON 응답 작성 (ByteBuf 메모리 누수 방지)
     *
     * <p>writeWith 실패 시 buffer를 명시적으로 해제합니다.
     */
    private Mono<Void> writeResponse(ServerWebExchange exchange, ApiResponse<Void> response) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(response);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse()
                    .writeWith(Mono.just(buffer))
                    .doOnError(
                            error -> {
                                // writeWith 실패 시 buffer 해제 (ByteBuf LEAK 방지)
                                org.springframework.core.io.buffer.DataBufferUtils.release(buffer);
                            });
        } catch (JsonProcessingException e) {
            return exchange.getResponse().setComplete();
        }
    }
}
