package com.ryuqq.gateway.adapter.in.gateway.filter;

import com.ryuqq.gateway.adapter.in.gateway.config.GatewayFilterOrder;
import com.ryuqq.gateway.adapter.in.gateway.trace.TraceIdMdcContext;
import com.ryuqq.gateway.application.trace.dto.command.GenerateTraceIdCommand;
import com.ryuqq.gateway.application.trace.port.in.command.GenerateTraceIdUseCase;
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
 *   <li>GenerateTraceIdUseCase를 통한 Trace-ID 생성
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

    /**
     * X-Trace-Id HTTP 헤더 이름
     */
    public static final String X_TRACE_ID_HEADER = "X-Trace-Id";

    /**
     * Exchange Attribute 키
     */
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
        return generateTraceIdUseCase
                .execute(new GenerateTraceIdCommand())
                .flatMap(response -> {
                    String traceId = response.traceId();

                    // 1. Request Header에 X-Trace-Id 추가 (Downstream 전달)
                    ServerHttpRequest mutatedRequest = exchange.getRequest()
                            .mutate()
                            .header(X_TRACE_ID_HEADER, traceId)
                            .build();

                    // 2. Exchange Attribute에 traceId 저장 (다른 Filter에서 사용)
                    exchange.getAttributes().put(TRACE_ID_ATTRIBUTE, traceId);

                    // 3. Response Header에 X-Trace-Id 추가 (Client 반환)
                    // beforeCommit을 사용하여 응답 전에 헤더 추가
                    exchange.getResponse().beforeCommit(() -> {
                        exchange.getResponse().getHeaders().add(X_TRACE_ID_HEADER, traceId);
                        return Mono.empty();
                    });

                    // 4. Reactor Context에 traceId 추가 (MDC 전파)
                    return chain.filter(exchange.mutate().request(mutatedRequest).build())
                            .contextWrite(ctx -> ctx.put(TraceIdMdcContext.TRACE_ID_KEY, traceId));
                });
    }
}
