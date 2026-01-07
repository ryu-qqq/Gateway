package com.ryuqq.gateway.adapter.in.gateway.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ryuqq.gateway.adapter.in.gateway.common.dto.ApiResponse;
import com.ryuqq.gateway.adapter.in.gateway.common.dto.ErrorInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Gateway Error Responder
 *
 * <p>Spring Cloud Gateway Filter에서 공통으로 사용하는 에러 응답 유틸리티
 *
 * <p><strong>지원 기능</strong>:
 *
 * <ul>
 *   <li>표준화된 JSON 에러 응답 생성
 *   <li>HTTP 상태 코드별 응답 메서드 제공
 *   <li>JsonProcessingException 로깅 처리
 * </ul>
 *
 * <p><strong>사용 예시</strong>:
 *
 * <pre>{@code
 * return errorResponder.forbidden(exchange, "MFA_REQUIRED", "MFA 인증이 필요합니다");
 * return errorResponder.unauthorized(exchange, "UNAUTHORIZED", "인증이 필요합니다");
 * return errorResponder.internalServerError(exchange, "INTERNAL_ERROR", "서버 오류");
 * }</pre>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class GatewayErrorResponder {

    private static final Logger log = LoggerFactory.getLogger(GatewayErrorResponder.class);

    private final ObjectMapper objectMapper;

    public GatewayErrorResponder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
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
     * 지정된 HTTP 상태 코드로 에러 응답 반환 (ByteBuf 메모리 누수 방지)
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
                    // 응답이 이미 커밋된 경우 스킵 (ReadOnlyHttpHeaders 예외 방지)
                    if (exchange.getResponse().isCommitted()) {
                        return Mono.empty();
                    }

                    exchange.getResponse().setStatusCode(status);
                    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

                    ErrorInfo error = new ErrorInfo(errorCode, message);
                    ApiResponse<Void> errorResponse = ApiResponse.ofFailure(error);

                    try {
                        byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
                        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
                        return exchange.getResponse()
                                .writeWith(Mono.just(buffer))
                                .doOnError(
                                        e -> {
                                            // writeWith 실패 시 buffer 해제 (ByteBuf LEAK 방지)
                                            org.springframework.core.io.buffer.DataBufferUtils
                                                    .release(buffer);
                                        })
                                .doOnCancel(
                                        () -> {
                                            // 클라이언트 연결 끊김/요청 취소 시 buffer 해제 (ByteBuf LEAK 방지)
                                            org.springframework.core.io.buffer.DataBufferUtils
                                                    .release(buffer);
                                        });
                    } catch (JsonProcessingException e) {
                        log.error(
                                "Failed to serialize error response: errorCode={}, message={},"
                                        + " exception={}",
                                errorCode,
                                message,
                                e.getMessage());
                        return exchange.getResponse().setComplete();
                    }
                });
    }
}
