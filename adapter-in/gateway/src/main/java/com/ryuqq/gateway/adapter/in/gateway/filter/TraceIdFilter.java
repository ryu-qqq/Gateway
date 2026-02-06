package com.ryuqq.gateway.adapter.in.gateway.filter;

import com.ryuqq.gateway.adapter.in.gateway.config.GatewayFilterOrder;
import com.ryuqq.gateway.adapter.in.gateway.trace.TraceIdMdcContext;
import com.ryuqq.gateway.application.trace.dto.command.GenerateTraceIdCommand;
import com.ryuqq.gateway.application.trace.port.in.command.GenerateTraceIdUseCase;
import com.ryuqq.gateway.domain.trace.exception.InvalidTraceIdException;
import com.ryuqq.gateway.domain.trace.id.TraceId;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * TraceIdFilter - Trace-ID 생성 및 전달 필터
 *
 * <p>Spring Cloud Gateway GlobalFilter로 Trace-ID를 생성하고 전파합니다.
 *
 * <p><strong>실행 순서</strong>: {@code HIGHEST_PRECEDENCE} (가장 먼저 실행)
 *
 * <p><strong>책임</strong>:
 *
 * <ol>
 *   <li>Request Header에서 기존 X-Trace-Id 확인 (분산 추적 연속성)
 *   <li>유효한 Trace-ID가 없으면 GenerateTraceIdUseCase를 통해 새로 생성
 *   <li>Request Header에 X-Trace-Id 추가 (Downstream 전달)
 *   <li>Exchange Attribute에 traceId 저장 (다른 Filter에서 사용)
 *   <li>Reactor Context에 traceId 추가 (MDC 전파)
 *   <li>Response Header에 X-Trace-Id 추가 (Client 반환)
 * </ol>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class TraceIdFilter implements GlobalFilter, Ordered {

    /** X-Trace-Id HTTP 헤더 이름 */
    public static final String X_TRACE_ID_HEADER = "X-Trace-Id";

    /** Exchange Attribute 키 */
    public static final String TRACE_ID_ATTRIBUTE = "traceId";

    private final GenerateTraceIdUseCase generateTraceIdUseCase;

    public TraceIdFilter(GenerateTraceIdUseCase generateTraceIdUseCase) {
        this.generateTraceIdUseCase = generateTraceIdUseCase;
    }

    @Override
    public int getOrder() {
        return GatewayFilterOrder.TRACE_ID_FILTER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return resolveTraceId(exchange)
                .flatMap(traceId -> propagateTraceId(exchange, chain, traceId));
    }

    /**
     * 기존 X-Trace-Id 헤더 확인 후 유효하면 재사용, 없거나 유효하지 않으면 새로 생성
     *
     * @param exchange ServerWebExchange
     * @return Trace-ID 문자열을 담은 Mono
     */
    private Mono<String> resolveTraceId(ServerWebExchange exchange) {
        String existingTraceId = exchange.getRequest().getHeaders().getFirst(X_TRACE_ID_HEADER);

        if (existingTraceId != null && !existingTraceId.isBlank()) {
            try {
                TraceId.from(existingTraceId);
                return Mono.just(existingTraceId);
            } catch (InvalidTraceIdException e) {
                // 유효하지 않은 헤더는 무시하고 새로 생성
                return generateNewTraceId();
            }
        }

        return generateNewTraceId();
    }

    /**
     * 새로운 Trace-ID 생성
     *
     * @return 생성된 Trace-ID 문자열을 담은 Mono
     */
    private Mono<String> generateNewTraceId() {
        return generateTraceIdUseCase
                .execute(new GenerateTraceIdCommand())
                .map(response -> response.traceId());
    }

    /**
     * Trace-ID를 Request/Response Header, Exchange Attribute, Reactor Context에 전파
     *
     * @param exchange ServerWebExchange
     * @param chain GatewayFilterChain
     * @param traceId Trace-ID 문자열
     * @return 필터 체인 처리 결과
     */
    private Mono<Void> propagateTraceId(
            ServerWebExchange exchange, GatewayFilterChain chain, String traceId) {
        // 1. Request Header에 X-Trace-Id 추가 (Downstream 전달)
        ServerHttpRequest mutatedRequest =
                exchange.getRequest().mutate().header(X_TRACE_ID_HEADER, traceId).build();

        // 2. Exchange Attribute에 traceId 저장 (다른 Filter에서 사용)
        exchange.getAttributes().put(TRACE_ID_ATTRIBUTE, traceId);

        // 3. Response Header에 X-Trace-Id 추가 (Client 반환)
        // Actuator 경로는 응답이 이미 커밋된 상태에서 beforeCommit이 호출될 수 있으므로 스킵
        String path = exchange.getRequest().getURI().getPath();
        if (!isActuatorPath(path)) {
            exchange.getResponse()
                    .beforeCommit(
                            () -> {
                                if (!exchange.getResponse().isCommitted()) {
                                    exchange.getResponse()
                                            .getHeaders()
                                            .add(X_TRACE_ID_HEADER, traceId);
                                }
                                return Mono.empty();
                            });
        }

        // 4. Reactor Context에 traceId 추가 (MDC 전파)
        return chain.filter(exchange.mutate().request(mutatedRequest).build())
                .contextWrite(ctx -> ctx.put(TraceIdMdcContext.TRACE_ID_KEY, traceId));
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
}
