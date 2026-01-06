package com.ryuqq.gateway.adapter.in.gateway.error;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ryuqq.gateway.adapter.in.gateway.common.dto.ApiResponse;
import com.ryuqq.gateway.adapter.in.gateway.common.dto.ErrorInfo;
import com.ryuqq.gateway.domain.authentication.exception.JwtExpiredException;
import com.ryuqq.gateway.domain.authentication.exception.JwtInvalidException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * JWT Error Handler
 *
 * <p>JWT 인증 관련 예외를 처리하는 Global Error Handler
 *
 * <p><strong>처리하는 예외</strong>:
 *
 * <ul>
 *   <li>JwtExpiredException → 401 Unauthorized
 *   <li>JwtInvalidException → 401 Unauthorized
 *   <li>기타 예외 → 500 Internal Server Error
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
@Order(-1)
public class JwtErrorHandler implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(JwtErrorHandler.class);

    private final ObjectMapper objectMapper;

    public JwtErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        HttpStatus status = getHttpStatus(ex);
        String errorCode = getErrorCode(ex);
        String traceId = exchange.getAttribute("traceId");
        String message = getErrorMessage(ex);

        ErrorInfo error = new ErrorInfo(errorCode, message);
        ApiResponse<Void> errorResponse =
                ApiResponse.ofFailure(error, traceId != null ? traceId : "unknown");

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize error response for exception: {}", ex.getMessage(), e);
            return exchange.getResponse().setComplete();
        }
    }

    private HttpStatus getHttpStatus(Throwable ex) {
        if (ex instanceof JwtExpiredException || ex instanceof JwtInvalidException) {
            return HttpStatus.UNAUTHORIZED;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String getErrorCode(Throwable ex) {
        if (ex instanceof JwtExpiredException) {
            return "JWT_EXPIRED";
        }
        if (ex instanceof JwtInvalidException) {
            return "JWT_INVALID";
        }
        return "INTERNAL_ERROR";
    }

    private String getErrorMessage(Throwable ex) {
        String message = ex.getMessage();
        if (message != null && !message.isBlank()) {
            return message;
        }

        // 메시지가 null이거나 빈 경우 기본 메시지 제공
        if (ex instanceof JwtExpiredException) {
            return "토큰이 만료되었습니다";
        }
        if (ex instanceof JwtInvalidException) {
            return "유효하지 않은 토큰입니다";
        }
        return "서버 내부 오류가 발생했습니다";
    }
}
