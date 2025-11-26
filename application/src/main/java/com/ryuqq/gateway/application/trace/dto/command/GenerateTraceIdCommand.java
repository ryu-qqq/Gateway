package com.ryuqq.gateway.application.trace.dto.command;

/**
 * GenerateTraceIdCommand - Trace-ID 생성 명령
 *
 * <p>Trace-ID 생성에는 별도 파라미터가 필요하지 않습니다. 시간 정보는 ClockHolder를 통해 제공됩니다.
 *
 * @author development-team
 * @since 1.0.0
 */
public record GenerateTraceIdCommand() {}
