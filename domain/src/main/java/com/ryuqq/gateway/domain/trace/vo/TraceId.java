package com.ryuqq.gateway.domain.trace.vo;

import com.ryuqq.gateway.domain.common.util.ClockHolder;
import com.ryuqq.gateway.domain.trace.exception.InvalidTraceIdException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * TraceId - 분산 추적을 위한 Trace-ID Value Object
 *
 * <p>Gateway 진입 시 생성되어 모든 서비스로 전파되는 고유 식별자입니다.
 *
 * <p><strong>형식:</strong> {@code {timestamp}-{UUID}}
 *
 * <ul>
 *   <li>Timestamp: yyyyMMddHHmmssSSS (17자, 밀리초 단위)
 *   <li>UUID: UUID v4 (36자, 소문자 hex + 하이픈)
 *   <li>총 길이: 54자
 * </ul>
 *
 * <p><strong>예시:</strong> {@code 20250124123456789-a1b2c3d4-e5f6-4789-abcd-ef0123456789}
 *
 * <p><strong>유일성 보장:</strong> Timestamp(밀리초) + UUID로 충돌 방지
 *
 * @param value Trace-ID 문자열
 * @author development-team
 * @since 1.0.0
 */
public record TraceId(String value) {

    private static final String TIMESTAMP_FORMAT = "yyyyMMddHHmmssSSS";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT);

    /**
     * Trace-ID 형식 검증 정규식
     *
     * <p>형식: 17자리 숫자 + 하이픈 + UUID v4
     */
    private static final Pattern TRACE_ID_PATTERN =
            Pattern.compile(
                    "^\\d{17}-[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$");

    private static final int EXPECTED_LENGTH = 54;

    /** Compact Constructor (기본 검증) */
    public TraceId {
        Objects.requireNonNull(value, "TraceId value cannot be null");
    }

    /**
     * 새로운 Trace-ID 생성
     *
     * <p>ClockHolder를 사용하여 현재 시각 기반의 Timestamp와 UUID를 조합합니다.
     *
     * @param clockHolder 시계 제공자
     * @return 새로운 TraceId 인스턴스
     * @throws NullPointerException clockHolder가 null인 경우
     */
    public static TraceId generate(ClockHolder clockHolder) {
        Objects.requireNonNull(clockHolder, "ClockHolder cannot be null");

        ZonedDateTime now = ZonedDateTime.now(clockHolder.clock());
        String timestamp = now.format(TIMESTAMP_FORMATTER);
        String uuid = UUID.randomUUID().toString().toLowerCase();

        String traceIdValue =
                new StringBuilder(EXPECTED_LENGTH)
                        .append(timestamp)
                        .append('-')
                        .append(uuid)
                        .toString();
        return new TraceId(traceIdValue);
    }

    /**
     * 문자열로부터 Trace-ID 복원
     *
     * <p>기존 Trace-ID 문자열을 검증하고 TraceId 객체로 변환합니다.
     *
     * @param value Trace-ID 문자열
     * @return TraceId 인스턴스
     * @throws InvalidTraceIdException 형식이 올바르지 않은 경우
     */
    public static TraceId from(String value) {
        validate(value);
        return new TraceId(value);
    }

    /**
     * Trace-ID 형식 검증
     *
     * @param value 검증할 Trace-ID 문자열
     * @throws InvalidTraceIdException 유효하지 않은 경우
     */
    private static void validate(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidTraceIdException(value);
        }

        if (value.length() != EXPECTED_LENGTH) {
            throw new InvalidTraceIdException(value);
        }

        if (!TRACE_ID_PATTERN.matcher(value).matches()) {
            throw new InvalidTraceIdException(value);
        }
    }

    /**
     * Timestamp 부분 추출
     *
     * @return Timestamp 문자열 (17자)
     */
    public String timestamp() {
        return value.substring(0, 17);
    }

    /**
     * UUID 부분 추출
     *
     * @return UUID 문자열 (36자)
     */
    public String uuid() {
        return value.substring(18);
    }

    @Override
    public String toString() {
        return "TraceId{" + "value='" + value + '\'' + '}';
    }
}
