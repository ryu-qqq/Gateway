package com.ryuqq.gateway.adapter.in.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ryuqq.gateway.adapter.in.gateway.common.dto.ApiResponse;
import com.ryuqq.gateway.adapter.in.gateway.common.dto.ErrorInfo;
import com.ryuqq.gateway.adapter.in.gateway.config.GatewayFilterOrder;
import com.ryuqq.gateway.application.authentication.dto.command.ValidateJwtCommand;
import com.ryuqq.gateway.application.authentication.port.in.command.ValidateJwtUseCase;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
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
 *   <li>MDC 설정 및 정리
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_ID_ATTRIBUTE = "userId";
    private static final String ROLES_ATTRIBUTE = "roles";

    private final ValidateJwtUseCase validateJwtUseCase;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(
            ValidateJwtUseCase validateJwtUseCase, ObjectMapper objectMapper) {
        this.validateJwtUseCase = validateJwtUseCase;
        this.objectMapper = objectMapper;
    }

    @Override
    public int getOrder() {
        return GatewayFilterOrder.JWT_AUTH_FILTER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return unauthorized(exchange);
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        return validateJwtUseCase
                .execute(new ValidateJwtCommand(token))
                .flatMap(
                        response -> {
                            if (!response.isValid()) {
                                return unauthorized(exchange);
                            }

                            var claims = response.jwtClaims();
                            exchange.getAttributes().put(USER_ID_ATTRIBUTE, claims.subject());
                            exchange.getAttributes().put(ROLES_ATTRIBUTE, claims.roles());

                            MDC.put(USER_ID_ATTRIBUTE, claims.subject());

                            return chain.filter(exchange).doFinally(signalType -> MDC.clear());
                        })
                .onErrorResume(e -> unauthorized(exchange));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ErrorInfo error = new ErrorInfo("UNAUTHORIZED", "인증이 필요합니다");
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
