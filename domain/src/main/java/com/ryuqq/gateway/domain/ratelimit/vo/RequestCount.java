package com.ryuqq.gateway.domain.ratelimit.vo;

import java.util.Objects;

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
 * @author development-team
 * @since 1.0.0
 */
public final class RequestCount {

    private final long value;

    private RequestCount(long value) {
        this.value = value;
    }

    /**
     * RequestCount 생성
     *
     * @param count 요청 횟수
     * @return RequestCount 인스턴스
     * @throws IllegalArgumentException count가 음수인 경우
     */
    public static RequestCount of(long count) {
        if (count < 0) {
            throw new IllegalArgumentException("count cannot be negative");
        }
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
     * 주어진 limit을 초과했는지 확인
     *
     * @param limit 최대 허용 요청 수
     * @return 초과 여부
     */
    public boolean isExceeded(int limit) {
        return value > limit;
    }

    /**
     * 요청 횟수 값 반환
     *
     * @return 요청 횟수
     */
    public long getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RequestCount that = (RequestCount) o;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "RequestCount{" + "value=" + value + '}';
    }
}
