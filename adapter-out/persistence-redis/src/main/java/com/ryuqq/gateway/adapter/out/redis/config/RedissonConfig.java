package com.ryuqq.gateway.adapter.out.redis.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonReactiveClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson Configuration (분산락 전용)
 *
 * <p>Redisson 클라이언트 설정 (분산락 전용)
 *
 * <p><strong>역할</strong>:
 *
 * <ul>
 *   <li>RedissonReactiveClient 생성
 *   <li>분산락(RLock) 지원
 *   <li>Pub/Sub 지원
 * </ul>
 *
 * <p><strong>Lock 전략</strong>:
 *
 * <ul>
 *   <li>Wait Time: 0초 (즉시 실패 - 동시 요청 거부)
 *   <li>Lease Time: 10초 (자동 해제 - 데드락 방지)
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 * @see LettuceConfig 캐싱 전용 설정
 */
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    /**
     * RedissonReactiveClient Bean
     *
     * <p>Reactive 환경에서 분산락 지원을 위한 클라이언트
     *
     * @return RedissonReactiveClient 분산락용 Reactive 클라이언트
     */
    @Bean(destroyMethod = "shutdown")
    public RedissonReactiveClient redissonReactiveClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + redisHost + ":" + redisPort)
                .setConnectionMinimumIdleSize(4)
                .setConnectionPoolSize(16)
                .setRetryAttempts(3)
                .setRetryInterval(1500);

        return Redisson.create(config).reactive();
    }
}
