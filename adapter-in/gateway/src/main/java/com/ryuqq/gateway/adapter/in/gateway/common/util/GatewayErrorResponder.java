package com.ryuqq.gateway.adapter.in.gateway.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ryuqq.gateway.adapter.in.gateway.common.dto.GatewayProblemDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Gateway Error Responder (RFC 7807 ProblemDetail)
 *
 * <p>Spring Cloud Gateway Filter에서 공통으로 사용하는 에러 응답 유틸리티
 *
 * <p><strong>RFC 7807 표준 준수</strong>:
 *
 * <ul>
 *   <li>Content-Type: application/problem+json
 *   <li>표준 필드: type, title, status, detail, instance
 *   <li>확장 필드: code, timestamp, requestId
 * </ul>
 *
 * <p><strong>사용 예시</strong>:
 *
 * <pre>{@code
 * return errorResponder.forbidden(exchange, "MFA_REQUIRED", "MFA 인증이 필요합니다");
 * return errorResponder.unauthorized(exchange, "UNAUTHORIZED", "인증이 필요합니다");
 * return errorResponder.tooManyRequests(exchange, "RATE_LIMIT_EXCEEDED", "요청이 너무 많습니다");
 * }</pre>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class GatewayErrorResponder {

    private static final Logger log = LoggerFactory.getLogger(GatewayErrorResponder.class);

    /** RFC 7807 표준 Content-Type */
    private static final MediaType APPLICATION_PROBLEM_JSON =
            MediaType.valueOf("application/problem+json");

    private final ObjectMapper objectMapper;

    public GatewayErrorResponder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 400 Bad Request 응답 반환
     *
     * @param exchange ServerWebExchange
     * @param errorCode 에러 코드
     * @param message 에러 메시지
     * @return Mono&lt;Void&gt;
     */
    public Mono<Void> badRequest(ServerWebExchange exchange, String errorCode, String message) {
        return respond(exchange, HttpStatus.BAD_REQUEST, errorCode, message);
    }

    /**
     * 401 Unauthorized 응답 반환
     *
     * @param exchange ServerWebExchange
     * @param errorCode 에러 코드
     * @param message 에러 메시지
     * @return Mono&lt;Void&gt;
     */
    public Mono<Void> unauthorized(ServerWebExchange exchange, String errorCode, String message) {
        return respond(exchange, HttpStatus.UNAUTHORIZED, errorCode, message);
    }

    /**
     * 403 Forbidden 응답 반환
     *
     * @param exchange ServerWebExchange
     * @param errorCode 에러 코드
     * @param message 에러 메시지
     * @return Mono&lt;Void&gt;
     */
    public Mono<Void> forbidden(ServerWebExchange exchange, String errorCode, String message) {
        return respond(exchange, HttpStatus.FORBIDDEN, errorCode, message);
    }

    /**
     * 429 Too Many Requests 응답 반환
     *
     * @param exchange ServerWebExchange
     * @param errorCode 에러 코드
     * @param message 에러 메시지
     * @return Mono&lt;Void&gt;
     */
    public Mono<Void> tooManyRequests(
            ServerWebExchange exchange, String errorCode, String message) {
        return respond(exchange, HttpStatus.TOO_MANY_REQUESTS, errorCode, message);
    }

    /**
     * 500 Internal Server Error 응답 반환
     *
     * @param exchange ServerWebExchange
     * @param errorCode 에러 코드
     * @param message 에러 메시지
     * @return Mono&lt;Void&gt;
     */
    public Mono<Void> internalServerError(
            ServerWebExchange exchange, String errorCode, String message) {
        return respond(exchange, HttpStatus.INTERNAL_SERVER_ERROR, errorCode, message);
    }

    /**
     * 지정된 HTTP 상태 코드로 RFC 7807 ProblemDetail 응답 반환
     *
     * <p>응답이 이미 커밋된 경우 스킵하고, writeWith 실패 시 buffer를 명시적으로 해제합니다.
     *
     * @param exchange ServerWebExchange
     * @param status HTTP 상태 코드
     * @param errorCode 에러 코드
     * @param message 에러 메시지
     * @return Mono&lt;Void&gt;
     */
    public Mono<Void> respond(
            ServerWebExchange exchange, HttpStatus status, String errorCode, String message) {
        return Mono.defer(
                () -> {
                    if (exchange.getResponse().isCommitted()) {
                        return Mono.empty();
                    }

                    String requestPath = exchange.getRequest().getPath().value();
                    String requestId = extractRequestId(exchange);

                    GatewayProblemDetail problemDetail =
                            GatewayProblemDetail.builder()
                                    .status(status.value())
                                    .title(status.getReasonPhrase())
                                    .detail(message)
                                    .code(errorCode)
                                    .instance(requestPath)
                                    .requestId(requestId)
                                    .build();

                    return writeResponse(exchange, status, problemDetail);
                });
    }

    /**
     * GatewayProblemDetail 객체로 직접 응답 반환
     *
     * @param exchange ServerWebExchange
     * @param status HTTP 상태 코드
     * @param problemDetail ProblemDetail 객체
     * @return Mono&lt;Void&gt;
     */
    public Mono<Void> respond(
            ServerWebExchange exchange, HttpStatus status, GatewayProblemDetail problemDetail) {
        return Mono.defer(
                () -> {
                    if (exchange.getResponse().isCommitted()) {
                        return Mono.empty();
                    }
                    return writeResponse(exchange, status, problemDetail);
                });
    }

    private Mono<Void> writeResponse(
            ServerWebExchange exchange, HttpStatus status, GatewayProblemDetail problemDetail) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(APPLICATION_PROBLEM_JSON);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(problemDetail);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse()
                    .writeWith(Mono.just(buffer))
                    .doOnError(e -> DataBufferUtils.release(buffer))
                    .doOnCancel(() -> DataBufferUtils.release(buffer));
        } catch (JsonProcessingException e) {
            log.error(
                    "Failed to serialize ProblemDetail: code={}, detail={}, exception={}",
                    problemDetail.code(),
                    problemDetail.detail(),
                    e.getMessage());
            return exchange.getResponse().setComplete();
        }
    }

    private String extractRequestId(ServerWebExchange exchange) {
        // traceId 또는 requestId 헤더에서 추출
        String traceId = exchange.getRequest().getHeaders().getFirst("X-Request-Id");
        if (traceId != null) {
            return traceId;
        }
        // Attribute에서 추출 시도
        Object attr = exchange.getAttribute("traceId");
        return attr != null ? attr.toString() : null;
    }
}
