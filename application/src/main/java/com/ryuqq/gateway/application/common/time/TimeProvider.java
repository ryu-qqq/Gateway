package com.ryuqq.gateway.application.common.time;

import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * 시간 제공자
 *
 * <p>시스템 시간을 제공하는 공통 컴포넌트입니다. Clock을 캡슐화하여 테스트 용이성을 보장합니다.
 *
 * <p><strong>책임:</strong>
 *
 * <ul>
 *   <li>현재 시간(Instant) 제공
 *   <li>Clock 의존성 캡슐화
 * </ul>
 *
 * <p><strong>사용 예시:</strong>
 *
 * <pre>{@code
 * @Service
 * public class UpdateService {
 *     private final TimeProvider timeProvider;
 *
 *     public void execute(Command command) {
 *         aggregate.update(data, timeProvider.now());
 *     }
 * }
 * }</pre>
 *
 * @author development-team
 * @since 1.0.0
 */
@Component
public class TimeProvider {

    private final Clock clock;

    public TimeProvider(Clock clock) {
        this.clock = clock;
    }

    /**
     * 현재 시간 반환
     *
     * @return 현재 Instant
     */
    public Instant now() {
        return clock.instant();
    }
}
