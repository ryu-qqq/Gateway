package com.ryuqq.gateway.application.trace.port.in.command;

import com.ryuqq.gateway.application.trace.dto.command.GenerateTraceIdCommand;
import com.ryuqq.gateway.application.trace.dto.response.GenerateTraceIdResponse;
import reactor.core.publisher.Mono;

/**
 * GenerateTraceIdUseCase - Trace-ID 생성 유스케이스
 *
 * <p>Gateway 진입 시 새로운 Trace-ID를 생성합니다.
 *
 * <p><strong>Trace-ID 형식:</strong> {@code {timestamp}-{UUID}}
 *
 * <ul>
 *   <li>Timestamp: yyyyMMddHHmmssSSS (17자)
 *   <li>UUID: UUID v4 (36자)
 *   <li>총 길이: 54자
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public interface GenerateTraceIdUseCase {

    /**
     * Trace-ID 생성
     *
     * @param command 생성 명령 (파라미터 없음)
     * @return Trace-ID 생성 응답
     */
    Mono<GenerateTraceIdResponse> execute(GenerateTraceIdCommand command);
}
