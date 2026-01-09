package com.ryuqq.gateway.adapter.in.gateway.error;

import com.ryuqq.gateway.adapter.in.gateway.common.util.GatewayErrorResponder;
import com.ryuqq.gateway.adapter.in.gateway.filter.TraceIdFilter;
import com.ryuqq.gateway.domain.authentication.exception.JwtExpiredException;
import com.ryuqq.gateway.domain.authentication.exception.JwtInvalidException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
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

    private final GatewayErrorResponder errorResponder;

    public JwtErrorHandler(GatewayErrorResponder errorResponder) {
        this.errorResponder = errorResponder;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        HttpStatus status = getHttpStatus(ex);
        String errorCode = getErrorCode(ex);
        String message = getErrorMessage(ex);

        // 500 에러만 Sentry 전송 (401은 비즈니스 에러, actuator는 내부 헬스체크이므로 제외)
        String path = exchange.getRequest().getPath().value();
        if (status == HttpStatus.INTERNAL_SERVER_ERROR && !isActuatorPath(path)) {
            String traceId = extractTraceId(exchange);
            log.error("Unexpected error occurred - traceId: {}, path: {}", traceId, path, ex);
        }

        return errorResponder.respond(exchange, status, errorCode, message);
    }

    private boolean isActuatorPath(String path) {
        return path != null && path.startsWith("/actuator");
    }

    private String extractTraceId(ServerWebExchange exchange) {
        return Optional.ofNullable(exchange.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE))
                .map(Object::toString)
                .orElseGet(
                        () ->
                                exchange.getRequest()
                                        .getHeaders()
                                        .getFirst(TraceIdFilter.X_TRACE_ID_HEADER));
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
