package com.ryuqq.connectly.bootstrap.config;

import com.ryuqq.connectly.application.common.config.SystemClockHolder;
import com.ryuqq.connectly.domain.common.util.ClockHolder;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Clock 및 ClockHolder Bean 설정
 *
 * <p>Bootstrap Layer에서 Infrastructure 관심사인 Clock 관련 Bean을 등록합니다.
 *
 * <p><strong>설계 원칙:</strong>
 *
 * <ul>
 *   <li>✅ Infrastructure 관심사는 Bootstrap에서 관리
 *   <li>✅ Clock은 SystemDefaultZone 사용 (Asia/Seoul 또는 UTC)
 *   <li>✅ ClockHolder는 DIP를 통해 Domain과 Application에서 사용
 *   <li>✅ 테스트 환경에서는 FixedClock으로 교체 가능
 * </ul>
 *
 * <p><strong>Bean 등록 전략:</strong>
 *
 * <ul>
 *   <li>Clock: System Default Zone 사용
 *   <li>ClockHolder: SystemClockHolder 구현체 사용
 * </ul>
 *
 * @author ryu-qqq
 * @since 2025-11-21
 */
@Configuration
public class ClockConfig {

    /**
     * System Clock Bean 등록
     *
     * <p>System Default Zone을 사용하는 Clock을 반환합니다.
     *
     * <p>애플리케이션 전역에서 Singleton으로 공유됩니다.
     *
     * @return System Default Zone Clock
     * @author ryu-qqq
     * @since 2025-11-21
     */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    /**
     * ClockHolder Bean 등록
     *
     * <p>SystemClockHolder 구현체를 반환합니다.
     *
     * <p>Domain, Application, Persistence 등 모든 레이어에서 주입받아 사용할 수 있습니다.
     *
     * @param clock System Clock Bean
     * @return SystemClockHolder 구현체
     * @author ryu-qqq
     * @since 2025-11-21
     */
    @Bean
    public ClockHolder clockHolder(Clock clock) {
        return new SystemClockHolder(clock);
    }
}
