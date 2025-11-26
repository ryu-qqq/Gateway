package com.ryuqq.gateway.application.trace.dto.response;

/**
 * GenerateTraceIdResponse - Trace-ID 생성 응답
 *
 * <p>생성된 Trace-ID 문자열을 포함합니다.
 *
 * @param traceId 생성된 Trace-ID (형식: {timestamp}-{UUID})
 * @author development-team
 * @since 1.0.0
 */
public record GenerateTraceIdResponse(String traceId) {}
