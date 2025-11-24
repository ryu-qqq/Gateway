package com.ryuqq.connectly.application.common.config;

import com.ryuqq.connectly.domain.common.util.ClockHolder;
import java.time.Clock;

/**
 * System Clock을 제공하는 ClockHolder 구현체
 *
 * <p>Production 환경에서 사용되는 실제 시스템 시간을 제공합니다.
 *
 * <p><strong>설계 원칙:</strong>
 *
 * <ul>
 *   <li>✅ ClockHolder 인터페이스 구현
 *   <li>✅ System Default Zone Clock 사용
 *   <li>✅ Immutable 객체 (final Clock)
 *   <li>✅ Spring Bean으로 등록
 * </ul>
 *
 * @author ryu-qqq
 * @since 2025-11-21
 */
public class SystemClockHolder implements ClockHolder {

    private final Clock clock;

    /**
     * SystemClockHolder 생성자
     *
     * @param clock System Clock
     * @author ryu-qqq
     * @since 2025-11-21
     */
    public SystemClockHolder(Clock clock) {
        this.clock = clock;
    }

    /**
     * System Clock 반환
     *
     * @return System Clock
     * @author ryu-qqq
     * @since 2025-11-21
     */
    @Override
    public Clock getClock() {
        return clock;
    }
}
