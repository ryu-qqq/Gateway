package com.ryuqq.gateway.adapter.in.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ryuqq.gateway.adapter.in.gateway.common.dto.ApiResponse;
import com.ryuqq.gateway.adapter.in.gateway.common.dto.ErrorInfo;
import com.ryuqq.gateway.adapter.in.gateway.common.util.ClientIpExtractor;
import com.ryuqq.gateway.adapter.in.gateway.config.GatewayFilterOrder;
import com.ryuqq.gateway.application.ratelimit.config.RateLimitProperties;
import com.ryuqq.gateway.application.ratelimit.dto.command.CheckRateLimitCommand;
import com.ryuqq.gateway.application.ratelimit.port.in.command.CheckRateLimitUseCase;
import com.ryuqq.gateway.domain.ratelimit.exception.IpBlockedException;
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
 * Rate Limit Filter
 *
 * <p>Spring Cloud Gateway GlobalFilter로 Rate Limiting을 수행합니다.
 *
 * <p><strong>책임</strong>:
 *
 * <ul>
 *   <li>IP 기반 Rate Limit 체크
 *   <li>Endpoint 기반 Rate Limit 체크
 *   <li>Rate Limit 초과 시 429 또는 403 응답
 *   <li>Rate Limit 헤더 추가 (X-RateLimit-Limit, X-RateLimit-Remaining, Retry-After)
 * </ul>
 *
 * <p><strong>실행 순서</strong>: TraceIdFilter 다음, JwtAuthenticationFilter 이전
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final String X_RATE_LIMIT_LIMIT_HEADER = "X-RateLimit-Limit";
    private static final String X_RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";
    private static final String RETRY_AFTER_HEADER = "Retry-After";
    private static final String RATE_LIMIT_CHECKED_ATTRIBUTE = "RATE_LIMIT_CHECKED";

    private final RateLimitProperties rateLimitProperties;
    private final CheckRateLimitUseCase checkRateLimitUseCase;
    private final ObjectMapper objectMapper;
    private final ClientIpExtractor clientIpExtractor;

    public RateLimitFilter(
            RateLimitProperties rateLimitProperties,
            CheckRateLimitUseCase checkRateLimitUseCase,
            ObjectMapper objectMapper,
            ClientIpExtractor clientIpExtractor) {
        this.rateLimitProperties = rateLimitProperties;
        this.checkRateLimitUseCase = checkRateLimitUseCase;
        this.objectMapper = objectMapper;
        this.clientIpExtractor = clientIpExtractor;
    }

    @Override
    public int getOrder() {
        return GatewayFilterOrder.RATE_LIMIT_FILTER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 이미 Rate Limit 체크를 수행한 경우 스킵 (중복 실행 방지)
        // Spring Cloud Gateway에서 동일 exchange가 여러 번 필터 체인을 통과할 수 있음
        Boolean rateLimitChecked = exchange.getAttribute(RATE_LIMIT_CHECKED_ATTRIBUTE);
        if (Boolean.TRUE.equals(rateLimitChecked)) {
            return chain.filter(exchange);
        }

        // Rate Limit 비활성화 시 스킵
        if (!rateLimitProperties.isEnabled()) {
            return chain.filter(exchange);
        }

        // Rate Limit 체크 시작 - 중복 실행 방지를 위해 플래그 설정
        exchange.getAttributes().put(RATE_LIMIT_CHECKED_ATTRIBUTE, true);

        // AWS 환경 (CloudFront → ALB → ECS)에서 X-Forwarded-For 헤더 사용
        String clientIp = clientIpExtractor.extractWithTrustedProxy(exchange);
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();

        // IP를 추출할 수 없는 경우 Rate Limit 스킵 (graceful degradation)
        // 모든 요청이 "unknown"으로 처리되면 전체 서비스가 429 에러 발생
        if (ClientIpExtractor.UNKNOWN_IP.equals(clientIp)) {
            log.warn(
                    "Unable to extract client IP for path={}. Skipping IP-based rate limit.", path);
            return chain.filter(exchange);
        }

        // IP 기반 Rate Limit 체크
        CheckRateLimitCommand ipCommand = CheckRateLimitCommand.forIp(clientIp);

        return checkRateLimitUseCase
                .execute(ipCommand)
                .flatMap(
                        ipResponse -> {
                            if (!ipResponse.allowed()) {
                                return tooManyRequests(
                                        exchange,
                                        ipResponse.limit(),
                                        ipResponse.retryAfterSeconds());
                            }

                            // Endpoint 기반 Rate Limit 체크
                            CheckRateLimitCommand endpointCommand =
                                    CheckRateLimitCommand.forEndpoint(path, method);

                            return checkRateLimitUseCase
                                    .execute(endpointCommand)
                                    .flatMap(
                                            endpointResponse -> {
                                                if (!endpointResponse.allowed()) {
                                                    return tooManyRequests(
                                                            exchange,
                                                            endpointResponse.limit(),
                                                            endpointResponse.retryAfterSeconds());
                                                }

                                                // Rate Limit 통과 - 다음 필터로 진행
                                                // 성공 응답의 Rate Limit 헤더는 나중에 별도 구현 예정
                                                return chain.filter(exchange);
                                            });
                        })
                .onErrorResume(
                        IpBlockedException.class, e -> forbidden(exchange, e.retryAfterSeconds()))
                .onErrorResume(
                        RateLimitExceededException.class,
                        e -> tooManyRequests(exchange, e.limit(), e.retryAfterSeconds()))
                .onErrorResume(
                        Exception.class,
                        e -> {
                            // Rate Limit 체크 실패 시 graceful degradation
                            // 서비스 가용성을 위해 Rate Limit 없이 요청 진행
                            log.warn(
                                    "Rate limit check failed for IP={}, path={}: {}",
                                    clientIp,
                                    path,
                                    e.getMessage());
                            return chain.filter(exchange);
                        });
    }

    /** 429 Too Many Requests 응답 */
    private Mono<Void> tooManyRequests(
            ServerWebExchange exchange, int limit, int retryAfterSeconds) {
        return Mono.defer(
                () -> {
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
                            new ErrorInfo("RATE_LIMIT_EXCEEDED", "요청 빈도가 너무 높습니다. 잠시 후 다시 시도해주세요.");
                    ApiResponse<Void> errorResponse = ApiResponse.ofFailure(error);

                    return writeResponse(exchange, errorResponse);
                });
    }

    /** 403 Forbidden 응답 (IP 차단) */
    private Mono<Void> forbidden(ServerWebExchange exchange, int retryAfterSeconds) {
        return Mono.defer(
                () -> {
                    if (exchange.getResponse().isCommitted()) {
                        return Mono.empty();
                    }
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                    exchange.getResponse()
                            .getHeaders()
                            .add(RETRY_AFTER_HEADER, String.valueOf(retryAfterSeconds));

                    ErrorInfo error =
                            new ErrorInfo("IP_BLOCKED", "비정상적인 요청 패턴이 감지되어 일시적으로 차단되었습니다.");
                    ApiResponse<Void> errorResponse = ApiResponse.ofFailure(error);

                    return writeResponse(exchange, errorResponse);
                });
    }

    /** JSON 응답 작성 */
    private Mono<Void> writeResponse(ServerWebExchange exchange, ApiResponse<Void> response) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(response);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            return exchange.getResponse().setComplete();
        }
    }
}
