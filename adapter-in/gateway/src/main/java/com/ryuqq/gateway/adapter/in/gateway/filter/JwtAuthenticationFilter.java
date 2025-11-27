package com.ryuqq.gateway.adapter.in.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ryuqq.gateway.adapter.in.gateway.common.dto.ApiResponse;
import com.ryuqq.gateway.adapter.in.gateway.common.dto.ErrorInfo;
import com.ryuqq.gateway.adapter.in.gateway.common.util.ClientIpExtractor;
import com.ryuqq.gateway.adapter.in.gateway.config.GatewayFilterOrder;
import com.ryuqq.gateway.application.authentication.dto.command.ValidateJwtCommand;
import com.ryuqq.gateway.application.authentication.port.in.command.ValidateJwtUseCase;
import com.ryuqq.gateway.application.ratelimit.dto.command.RecordFailureCommand;
import com.ryuqq.gateway.application.ratelimit.port.in.command.RecordFailureUseCase;
import java.util.HashSet;
import java.util.Set;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
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
 *   <li>Downstream 서비스로 X-User-Id 헤더 전달
 *   <li>Reactor Context에 userId 저장 (로깅용)
 *   <li>JWT 검증 실패 시 RecordFailureUseCase 호출 (IP 차단용)
 * </ul>
 *
 * <p><strong>주의</strong>: Reactive 환경에서는 ThreadLocal 기반 MDC 대신 Reactor Context를 사용해야 합니다.
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_ID_ATTRIBUTE = "userId";
    private static final String TENANT_ID_ATTRIBUTE = "tenantId";
    private static final String PERMISSION_HASH_ATTRIBUTE = "permissionHash";
    private static final String ROLES_ATTRIBUTE = "roles";
    private static final String MFA_VERIFIED_ATTRIBUTE = "mfaVerified";
    private static final String X_USER_ID_HEADER = "X-User-Id";

    private final ValidateJwtUseCase validateJwtUseCase;
    private final RecordFailureUseCase recordFailureUseCase;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(
            ValidateJwtUseCase validateJwtUseCase,
            RecordFailureUseCase recordFailureUseCase,
            ObjectMapper objectMapper) {
        this.validateJwtUseCase = validateJwtUseCase;
        this.recordFailureUseCase = recordFailureUseCase;
        this.objectMapper = objectMapper;
    }

    @Override
    public int getOrder() {
        return GatewayFilterOrder.JWT_AUTH_FILTER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        // Authorization 헤더가 없거나 Bearer prefix가 없는 경우:
        // Invalid JWT 공격이 아니므로 실패 기록 없이 단순히 401 반환
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return unauthorized(exchange);
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        return validateJwtUseCase
                .execute(new ValidateJwtCommand(token))
                .flatMap(
                        response -> {
                            // 실제 JWT 토큰이 있지만 검증 실패한 경우에만 Invalid JWT로 기록
                            if (!response.isValid()) {
                                return recordFailureAndUnauthorized(exchange);
                            }

                            var claims = response.jwtClaims();
                            String userId = claims.subject();

                            // Exchange Attribute 설정 (Gateway 내부 사용)
                            exchange.getAttributes().put(USER_ID_ATTRIBUTE, userId);
                            exchange.getAttributes().put(TENANT_ID_ATTRIBUTE, claims.tenantId());
                            exchange.getAttributes()
                                    .put(PERMISSION_HASH_ATTRIBUTE, claims.permissionHash());
                            Set<String> rolesSet = new HashSet<>(claims.roles());
                            exchange.getAttributes().put(ROLES_ATTRIBUTE, rolesSet);
                            exchange.getAttributes()
                                    .put(MFA_VERIFIED_ATTRIBUTE, claims.mfaVerified());

                            // Downstream 서비스로 userId 전달 (Header)
                            ServerHttpRequest mutatedRequest =
                                    exchange.getRequest()
                                            .mutate()
                                            .header(X_USER_ID_HEADER, userId)
                                            .build();

                            ServerWebExchange mutatedExchange =
                                    exchange.mutate().request(mutatedRequest).build();

                            // Reactor Context에 userId 저장 (로깅 컨텍스트 전파)
                            return chain.filter(mutatedExchange)
                                    .contextWrite(ctx -> ctx.put(USER_ID_ATTRIBUTE, userId));
                        })
                .onErrorResume(e -> recordFailureAndUnauthorized(exchange));
    }

    /**
     * Invalid JWT 실패 기록 후 401 응답 반환
     *
     * <p>RecordFailureUseCase를 호출하여 IP별 실패 횟수를 증가시킵니다. 임계값 초과 시 IP가 차단됩니다.
     */
    private Mono<Void> recordFailureAndUnauthorized(ServerWebExchange exchange) {
        String clientIp = ClientIpExtractor.extract(exchange);
        RecordFailureCommand command = RecordFailureCommand.forInvalidJwt(clientIp);

        return recordFailureUseCase.execute(command).then(unauthorized(exchange));
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
