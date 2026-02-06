package com.ryuqq.gateway.domain.tenant.vo;

/**
 * MaxActiveSessions - 최대 동시 세션 수 Value Object
 *
 * <p>테넌트별 최대 동시 세션 수를 나타내는 불변 객체입니다.
 *
 * @param value 최대 동시 세션 수 (양수)
 * @author development-team
 * @since 1.0.0
 */
public record MaxActiveSessions(int value) {

    private static final int DEFAULT_VALUE = 5;

    public MaxActiveSessions {
        if (value <= 0) {
            throw new IllegalArgumentException("maxActiveSessions must be positive");
        }
    }

    public static MaxActiveSessions of(int value) {
        return new MaxActiveSessions(value);
    }

    public static MaxActiveSessions defaultValue() {
        return new MaxActiveSessions(DEFAULT_VALUE);
    }

    public boolean canCreateNewSession(int currentSessionCount) {
        return currentSessionCount < value;
    }
}
