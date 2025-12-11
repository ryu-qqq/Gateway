package com.ryuqq.gateway.domain.ratelimit.vo;

/**
 * RequestCount - 요청 횟수 Value Object
 *
 * <p>Rate Limiting에서 현재 요청 횟수를 나타내는 불변 객체입니다.
 *
 * <p><strong>특징:</strong>
 *
 * <ul>
 *   <li>불변 객체 (Immutable)
 *   <li>0 이상의 양수만 허용
 *   <li>increment() 메서드로 새 인스턴스 반환
 * </ul>
 *
 * @param value 요청 횟수
 * @author development-team
 * @since 1.0.0
 */
public record RequestCount(long value) {

    /** Compact Constructor (검증 로직) */
    public RequestCount {
        if (value < 0) {
            throw new IllegalArgumentException("count cannot be negative");
        }
    }

    /**
     * RequestCount 생성
     *
     * @param count 요청 횟수
     * @return RequestCount 인스턴스
     * @throws IllegalArgumentException count가 음수인 경우
     */
    public static RequestCount of(long count) {
        return new RequestCount(count);
    }

    /**
     * 0 카운트 생성
     *
     * @return 0 값의 RequestCount 인스턴스
     */
    public static RequestCount zero() {
        return new RequestCount(0);
    }

    /**
     * 카운트를 1 증가시킨 새 인스턴스 반환
     *
     * <p>원본 객체는 변경되지 않습니다.
     *
     * @return 1 증가된 새 RequestCount 인스턴스
     */
    public RequestCount increment() {
        return new RequestCount(value + 1);
    }

    /**
     * 주어진 limit에 도달했거나 초과했는지 확인
     *
     * <p>limit에 도달하면 초과로 판단합니다. 예를 들어 limit=10인 경우, 10번째 요청부터 차단됩니다.
     *
     * @param limit 최대 허용 요청 수
     * @return 초과 여부
     */
    public boolean isExceeded(int limit) {
        return value >= limit;
    }

    @Override
    public String toString() {
        return "RequestCount{" + "value=" + value + '}';
    }
}
