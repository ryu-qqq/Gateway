package com.ryuqq.gateway.application.trace.service.command;

import com.ryuqq.gateway.application.trace.dto.command.GenerateTraceIdCommand;
import com.ryuqq.gateway.application.trace.dto.response.GenerateTraceIdResponse;
import com.ryuqq.gateway.application.trace.port.in.command.GenerateTraceIdUseCase;
import com.ryuqq.gateway.domain.common.util.ClockHolder;
import com.ryuqq.gateway.domain.trace.vo.TraceId;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * GenerateTraceIdService - Trace-ID 생성 서비스
 *
 * <p>ClockHolder를 사용하여 새로운 Trace-ID를 생성합니다.
 *
 * <p><strong>비즈니스 로직:</strong>
 *
 * <ol>
 *   <li>ClockHolder에서 현재 시각 추출
 *   <li>UUID 생성
 *   <li>Trace-ID 조합 ({timestamp}-{UUID})
 * </ol>
 *
 * <p><strong>Transaction:</strong> 불필요 (Stateless 연산)
 *
 * @author development-team
 * @since 1.0.0
 */
@Service
public class GenerateTraceIdService implements GenerateTraceIdUseCase {

    private final ClockHolder clockHolder;

    public GenerateTraceIdService(ClockHolder clockHolder) {
        this.clockHolder = clockHolder;
    }

    @Override
    public Mono<GenerateTraceIdResponse> execute(GenerateTraceIdCommand command) {
        return Mono.fromCallable(
                () -> {
                    TraceId traceId = TraceId.generate(clockHolder);
                    return new GenerateTraceIdResponse(traceId.getValue());
                });
    }
}
