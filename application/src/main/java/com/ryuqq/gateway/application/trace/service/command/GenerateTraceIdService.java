package com.ryuqq.gateway.application.trace.service.command;

import com.ryuqq.gateway.application.trace.dto.command.GenerateTraceIdCommand;
import com.ryuqq.gateway.application.trace.dto.response.GenerateTraceIdResponse;
import com.ryuqq.gateway.application.trace.factory.TraceIdFactory;
import com.ryuqq.gateway.application.trace.port.in.command.GenerateTraceIdUseCase;
import com.ryuqq.gateway.domain.trace.id.TraceId;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * GenerateTraceIdService - Trace-ID 생성 서비스
 *
 * @author development-team
 * @since 1.0.0
 */
@Service
public class GenerateTraceIdService implements GenerateTraceIdUseCase {

    private final TraceIdFactory traceIdFactory;

    public GenerateTraceIdService(TraceIdFactory traceIdFactory) {
        this.traceIdFactory = traceIdFactory;
    }

    @Override
    public Mono<GenerateTraceIdResponse> execute(GenerateTraceIdCommand command) {
        return Mono.fromCallable(
                () -> {
                    TraceId traceId = traceIdFactory.create();
                    return new GenerateTraceIdResponse(traceId.value());
                });
    }
}
