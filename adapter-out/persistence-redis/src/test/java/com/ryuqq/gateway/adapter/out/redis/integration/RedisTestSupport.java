package com.ryuqq.gateway.adapter.out.redis.integration;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Redis Integration Test 기반 클래스
 *
 * <p>TestContainers를 사용하여 실제 Redis와 통합 테스트를 수행합니다.
 *
 * <p><strong>특징</strong>:
 *
 * <ul>
 *   <li>TestContainers Redis 7.2 사용
 *   <li>테스트 격리 - 각 테스트 후 DB flush
 *   <li>SpringJUnitConfig로 경량 컨텍스트 로딩
 * </ul>
 *
 * @author development-team
 * @since 1.0.0
 */
@SpringJUnitConfig(classes = RedisIntegrationTestConfig.class)
public abstract class RedisTestSupport {

    private static final int REDIS_PORT = 6379;

    static GenericContainer<?> redisContainer;

    static {
        redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
                .withExposedPorts(REDIS_PORT)
                .withCommand("redis-server", "--appendonly", "yes");
        redisContainer.start();
    }

    @Autowired
    protected ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    @Autowired
    protected LettuceConnectionFactory connectionFactory;

    @DynamicPropertySource
    static void configureRedisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(REDIS_PORT));
    }

    @AfterEach
    void cleanUp() {
        reactiveStringRedisTemplate
                .getConnectionFactory()
                .getReactiveConnection()
                .serverCommands()
                .flushDb()
                .block();
    }
}
