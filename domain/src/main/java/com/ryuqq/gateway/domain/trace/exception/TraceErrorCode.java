package com.ryuqq.gateway.domain.trace.exception;

import com.ryuqq.gateway.domain.common.exception.ErrorCode;

/**
 * TraceErrorCode - Trace Bounded Context 에러 코드
 *
 * <p>Trace-ID 도메인에서 발생하는 모든 비즈니스 예외의 에러 코드를 정의합니다.
 *
 * <p><strong>에러 코드 규칙:</strong>
 *
 * <ul>
 *   <li>✅ 형식: TRACE-{3자리 숫자}
 *   <li>✅ HTTP 상태 코드 매핑
 *   <li>✅ 명확한 에러 메시지
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
public enum TraceErrorCode implements ErrorCode {

    /**
     * Trace-ID 형식 오류
     *
     * <p>Trace-ID가 {timestamp}-{UUID} 형식을 따르지 않는 경우 발생
     */
    INVALID_TRACE_ID("TRACE-001", 400, "Invalid Trace-ID format");

    private final String code;
    private final int httpStatus;
    private final String message;

    TraceErrorCode(String code, int httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public int getHttpStatus() {
        return httpStatus;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
