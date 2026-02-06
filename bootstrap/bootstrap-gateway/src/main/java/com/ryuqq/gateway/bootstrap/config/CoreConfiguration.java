package com.ryuqq.gateway.bootstrap.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Core Configuration.
 *
 * <p>애플리케이션 핵심 빈들을 등록합니다.
 */
@Configuration
public class CoreConfiguration {

    /**
     * Clock 빈 등록.
     *
     * <p>시스템 기본 타임존 Clock을 제공합니다. 테스트 시 Mock Clock으로 교체 가능합니다.
     *
     * @return Clock 인스턴스
     */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
