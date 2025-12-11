package com.ryuqq.gateway.domain.trace.exception;

import com.ryuqq.gateway.domain.common.exception.DomainException;

/**
 * InvalidTraceIdException - Trace-ID 형식 오류 예외
 *
 * <p>Trace-ID가 {timestamp}-{UUID} 형식을 따르지 않는 경우 발생하는 예외입니다.
 *
 * <p><strong>발생 조건:</strong>
 *
 * <ul>
 *   <li>Trace-ID가 null 또는 빈 문자열인 경우
 *   <li>Trace-ID 형식이 올바르지 않은 경우 (정규식 불일치)
 *   <li>Timestamp 부분이 17자리가 아닌 경우
 *   <li>UUID 부분이 올바른 형식이 아닌 경우
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public final class InvalidTraceIdException extends DomainException {

    /**
     * 기본 생성자
     *
     * <p>기본 에러 메시지를 사용합니다.
     */
    public InvalidTraceIdException() {
        super(TraceErrorCode.INVALID_TRACE_ID);
    }

    /**
     * 상세 메시지를 포함한 생성자
     *
     * @param traceId 유효하지 않은 Trace-ID 값
     */
    public InvalidTraceIdException(String traceId) {
        super(TraceErrorCode.INVALID_TRACE_ID, "traceId=" + traceId);
    }
}
