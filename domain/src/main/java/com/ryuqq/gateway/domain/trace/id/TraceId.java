package com.ryuqq.gateway.domain.trace.id;

import com.ryuqq.gateway.domain.trace.exception.InvalidTraceIdException;
import java.util.regex.Pattern;

/**
 * TraceId - 분산 추적을 위한 Trace-ID Value Object
 *
 * <p>Gateway 진입 시 생성되어 모든 서비스로 전파되는 고유 식별자입니다.
 *
 * <p><strong>형식:</strong> {timestamp}-{uuid}
 *
 * <ul>
 *   <li>timestamp: 17자리 숫자 (yyyyMMddHHmmssSSS)
 *   <li>uuid: 표준 UUID 형식 (8-4-4-4-12)
 * </ul>
 *
 * <p><strong>예시:</strong> 20250124123456789-a1b2c3d4-e5f6-4789-abcd-ef0123456789
 *
 * @param value Trace-ID 문자열
 * @author development-team
 * @since 1.0.0
 */
public record TraceId(String value) {

    /** TraceId 형식 패턴: {17-digit timestamp}-{uuid} */
    private static final Pattern TRACE_ID_PATTERN =
            Pattern.compile(
                    "^\\d{17}-[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$",
                    Pattern.CASE_INSENSITIVE);

    public static TraceId of(String timestamp, String uuid) {
        return new TraceId(timestamp + "-" + uuid);
    }

    /**
     * 문자열로부터 TraceId 생성 (유효성 검증 포함)
     *
     * @param value Trace-ID 문자열
     * @return TraceId 인스턴스
     * @throws InvalidTraceIdException 형식이 올바르지 않은 경우
     */
    public static TraceId from(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidTraceIdException(value);
        }
        if (!TRACE_ID_PATTERN.matcher(value).matches()) {
            throw new InvalidTraceIdException(value);
        }
        return new TraceId(value);
    }

    public String timestamp() {
        return value.substring(0, 17);
    }
}
